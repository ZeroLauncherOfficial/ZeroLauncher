/*
 * ZeroLauncher
 * Copyright (C) 2020  Zero <Zero@zerolauncher.net> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.zero.launcher.launch;

import org.jetbrains.annotations.NotNullByDefault;
import org.zero.launcher.event.EventBus;
import org.zero.launcher.event.JVMLaunchFailedEvent;
import org.zero.launcher.event.ProcessExitedAbnormallyEvent;
import org.zero.launcher.event.ProcessStoppedEvent;
import org.zero.launcher.util.Log4jLevel;
import org.zero.launcher.util.StringUtils;
import org.zero.launcher.util.platform.ManagedProcess;
import org.zero.launcher.util.platform.OperatingSystem;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/// Monitors a running Minecraft process until exit, detecting exit type and failure events.
@NotNullByDefault
final class ExitWaiter implements Runnable {

    private final ManagedProcess process;
    private final Collection<Thread> joins;
    private final BiConsumer<Integer, ProcessListener.ExitType> watcher;

    /// Constructor.
    ///
    /// @param process the process to wait for
    /// @param joins threads to join before exit inspection
    /// @param watcher the callback that will be called after process stops.
    public ExitWaiter(ManagedProcess process, Collection<Thread> joins, BiConsumer<Integer, ProcessListener.ExitType> watcher) {
        this.process = process;
        this.joins = joins;
        this.watcher = watcher;
    }

    @Override
    public void run() {
        try {
            int exitCode = process.getProcess().waitFor();

            for (Thread thread : joins)
                thread.join();

            List<String> allLines = process.getLines(null);
            List<String> errorLines = process.getLines(Log4jLevel::guessLogLineError);
            ProcessListener.ExitType exitType;

            // JVM startup failures (e.g. invalid memory configuration) are emitted to raw stderr without Log4j formatting.
            if (exitCode != 0 && (StringUtils.containsOne(allLines,
                    "Could not create the Java Virtual Machine.",
                    "Error occurred during initialization of VM",
                    "A fatal exception has occurred. Program will exit.",
                    "Unrecognized option:",
                    "Invalid maximum heap size") || StringUtils.containsOne(errorLines,
                    "Could not create the Java Virtual Machine.",
                    "Error occurred during initialization of VM",
                    "A fatal exception has occurred. Program will exit."))) {
                EventBus.EVENT_BUS.fireEvent(new JVMLaunchFailedEvent(this, process));
                exitType = ProcessListener.ExitType.JVM_ERROR;
            } else if (exitCode != 0 || StringUtils.containsOne(errorLines,
                    "Crash report saved to", "Could not save crash report to", "This crash report has been saved to:",
                    "Unable to launch", "An exception was thrown, the game will display an error screen and halt.")
                    || StringUtils.containsOne(allLines, "Crash report saved to", "Could not save crash report to", "This crash report has been saved to:")) {
                EventBus.EVENT_BUS.fireEvent(new ProcessExitedAbnormallyEvent(this, process));

                if (exitCode == 137 && OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                    exitType = ProcessListener.ExitType.SIGKILL;
                } else {
                    exitType = ProcessListener.ExitType.APPLICATION_ERROR;
                }
            } else {
                exitType = ProcessListener.ExitType.NORMAL;
            }

            EventBus.EVENT_BUS.fireEvent(new ProcessStoppedEvent(this, process));

            watcher.accept(exitCode, exitType);
        } catch (InterruptedException e) {
            watcher.accept(1, ProcessListener.ExitType.INTERRUPTED);
        }
    }

}
