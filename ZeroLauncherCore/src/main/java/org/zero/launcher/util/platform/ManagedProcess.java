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
package org.zero.launcher.util.platform;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.zero.launcher.launch.StreamPump;
import org.zero.launcher.util.Lang;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/// Manages an operating system process and tracks output lines and associated monitor threads.
@NotNullByDefault
public final class ManagedProcess {
    /// Maximum number of log lines retained in memory.
    public static final int MAX_LOG_LINES = 2048;

    private final ReentrantLock lock = new ReentrantLock();
    private final Process process;
    private final List<String> commands;
    private final @Nullable String classpath;
    private final Map<String, Object> properties = new HashMap<>();
    private final Deque<String> lines = new ArrayDeque<>(MAX_LOG_LINES);
    private final List<Thread> relatedThreads = new ArrayList<>();

    public ManagedProcess(ProcessBuilder processBuilder) throws IOException {
        this.process = processBuilder.start();
        this.commands = List.copyOf(processBuilder.command());
        this.classpath = null;
    }

    /// Constructor.
    ///
    /// @param process  the raw system process that this instance manages.
    /// @param commands the command line of `process`.
    public ManagedProcess(Process process, List<String> commands) {
        this.process = process;
        this.commands = List.copyOf(commands);
        this.classpath = null;
    }

    /// Constructor.
    ///
    /// @param process   the raw system process that this instance manages.
    /// @param commands  the command line of `process`.
    /// @param classpath the classpath of java process
    public ManagedProcess(Process process, List<String> commands, @Nullable String classpath) {
        this.process = process;
        this.commands = List.copyOf(commands);
        this.classpath = classpath;
    }

    /// The raw system process that this instance manages.
    ///
    /// @return process
    public Process getProcess() {
        return process;
    }

    /// The command line.
    ///
    /// @return the unmodifiable list of each part of command line separated by spaces.
    public @Unmodifiable List<String> getCommands() {
        return commands;
    }

    /// The classpath.
    ///
    /// @return classpath
    public @Nullable String getClasspath() {
        return classpath;
    }

    /// Saves arbitrary metadata for this process.
    public Map<String, Object> getProperties() {
        return properties;
    }

    /// The standard output/error lines matching the given filter.
    ///
    /// @param lineFilter predicate to filter lines, or `null` for all lines
    /// @return unmodifiable list of lines
    public @Unmodifiable List<String> getLines(@Nullable Predicate<String> lineFilter) {
        lock.lock();
        try {
            if (lineFilter == null)
                return List.copyOf(lines);

            ArrayList<String> res = new ArrayList<>();
            for (String line : this.lines) {
                if (lineFilter.test(line))
                    res.add(line);
            }
            return Collections.unmodifiableList(res);
        } finally {
            lock.unlock();
        }
    }

    /// Appends a log line to the bounded history buffer.
    public void addLine(String line) {
        lock.lock();
        try {
            if (lines.size() >= MAX_LOG_LINES) {
                lines.removeFirst();
            }
            lines.addLast(line);
        } finally {
            lock.unlock();
        }
    }

    /// Adds a monitor thread associated with this process.
    public void addRelatedThread(Thread thread) {
        lock.lock();
        try {
            relatedThreads.add(thread);
        } finally {
            lock.unlock();
        }
    }

    /// Starts an input stream pump for stdout.
    public void pumpInputStream(Consumer<String> onLogLine) {
        addRelatedThread(Lang.thread(new StreamPump(process.getInputStream(), onLogLine, OperatingSystem.NATIVE_CHARSET), "ProcessInputStreamPump", true));
    }

    /// Starts an error stream pump for stderr.
    public void pumpErrorStream(Consumer<String> onLogLine) {
        addRelatedThread(Lang.thread(new StreamPump(process.getErrorStream(), onLogLine, OperatingSystem.NATIVE_CHARSET), "ProcessErrorStreamPump", true));
    }

    /// Returns `true` if the managed process is currently running.
    public boolean isRunning() {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /// The exit code of raw process.
    public int getExitCode() {
        return process.exitValue();
    }

    /// Destroys the raw process and other related threads that are monitoring this raw process.
    public void stop() {
        process.destroy();
        destroyRelatedThreads();
    }

    /// Interrupts all monitor threads associated with this process.
    public void destroyRelatedThreads() {
        lock.lock();
        try {
            relatedThreads.forEach(Thread::interrupt);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "ManagedProcess[commands=" + commands + ", isRunning=" + isRunning() + "]";
    }
}
