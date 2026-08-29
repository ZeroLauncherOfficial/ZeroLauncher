/*
 * ZeroLauncher
 * Copyright (C) 2026 Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.hmcl.guard;

import javafx.application.Platform;
import org.zero.hmcl.game.GameRepository;
import org.zero.hmcl.ui.guard.ZeroGuardDialog;
import org.zero.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class ZeroGuard {

    private ZeroGuard() {
    }

    public static void checkAndLaunch(GameRepository repository, String versionId, Runnable launchAction) {
        ZeroGuardReport report;
        try {
            report = ZeroGuardScanner.scan(repository, versionId);
        } catch (Throwable e) {
            Logger.LOG.warning("ZeroGuard: Scanning encountered error: " + e.getMessage());
            launchAction.run();
            return;
        }

        if (report.isClean() || !report.hasErrors()) {
            Logger.LOG.info("ZeroGuard: Instance [" + versionId + "] passed check with 0 critical errors. Launching.");
            launchAction.run();
        } else {
            Logger.LOG.warning("ZeroGuard: Instance [" + versionId + "] has " + report.getErrorCount() + " errors. Prompting ZeroGuard dialog.");
            Platform.runLater(() -> ZeroGuardDialog.show(repository, versionId, report, launchAction));
        }
    }
}
