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
package org.zero.launcher.download;

import org.zero.launcher.game.Version;
import org.zero.launcher.task.Task;
import org.zero.launcher.util.function.ExceptionalFunction;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author Zero
 */
public class DefaultGameBuilder extends GameBuilder {

    private final DefaultDependencyManager dependencyManager;

    public DefaultGameBuilder(DefaultDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    public DefaultDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    public Task<?> buildAsync() {
        var hints = new ArrayList<Task.StagesHint>();

        Task<Version> libraryTask = Task.supplyAsync(() -> new Version(name));
        libraryTask = libraryTask.thenComposeAsync(libraryTaskHelper(gameVersion, "game", gameVersion));
        hints.add(new Task.StagesHint("zero.install.game:" + gameVersion));
        hints.add(new Task.StagesHint("zero.install.libraries"));
        hints.add(new Task.StagesHint("zero.install.assets"));

        for (Map.Entry<String, String> entry : toolVersions.entrySet()) {
            libraryTask = libraryTask.thenComposeAsync(libraryTaskHelper(gameVersion, entry.getKey(), entry.getValue()));
            hints.add(new Task.StagesHint(String.format("zero.install.%s:%s", entry.getKey(), entry.getValue())));
        }

        for (RemoteVersion remoteVersion : remoteVersions) {
            libraryTask = libraryTask.thenComposeAsync(version -> dependencyManager.installLibraryAsync(version, remoteVersion));
            hints.add(new Task.StagesHint(String.format("zero.install.%s:%s", remoteVersion.getLibraryId(), remoteVersion.getSelfVersion())));
        }

        boolean isNew = !dependencyManager.getGameRepository().hasVersion(name);
        return libraryTask.thenComposeAsync(dependencyManager.getGameRepository()::saveAsync).whenComplete(exception -> {
            if (isNew && exception != null)
                dependencyManager.getGameRepository().removeVersionFromDisk(name);
        }).withStagesHints(hints);
    }

    private ExceptionalFunction<Version, Task<Version>, ?> libraryTaskHelper(String gameVersion, String libraryId, String libraryVersion) {
        return version -> dependencyManager.installLibraryAsync(gameVersion, version, libraryId, libraryVersion);
    }
}
