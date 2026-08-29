/*
 * ZeroLauncher
 * Copyright (C) 2022  Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.launcher.download.legacyfabric;

import org.zero.launcher.download.DefaultDependencyManager;
import org.zero.launcher.download.LibraryAnalyzer;
import org.zero.launcher.download.RemoteVersion;
import org.zero.launcher.game.Version;
import org.zero.launcher.task.Task;

import java.util.List;

public class LegacyFabricRemoteVersion extends RemoteVersion {
    /**
     * Constructor.
     *
     * @param gameVersion the Minecraft version that this remote version suits.
     * @param selfVersion the version string of the remote version.
     * @param urls        the installer or universal jar original URL.
     */
    LegacyFabricRemoteVersion(String gameVersion, String selfVersion, List<String> urls) {
        super(LibraryAnalyzer.LibraryType.LEGACY_FABRIC.getPatchId(), gameVersion, selfVersion, null, urls);
    }

    @Override
    public Task<Version> getInstallTask(DefaultDependencyManager dependencyManager, Version baseVersion) {
        return new LegacyFabricInstallTask(dependencyManager, baseVersion, this);
    }
}
