/*
 * ZeroLauncher
 * Copyright (C) 2021  Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.launcher.download.game;

import org.zero.launcher.download.DefaultDependencyManager;
import org.zero.launcher.download.LibraryAnalyzer;
import org.zero.launcher.download.RemoteVersion;
import org.zero.launcher.game.ReleaseType;
import org.zero.launcher.game.Version;
import org.zero.launcher.task.Task;
import org.zero.launcher.util.Immutable;
import org.zero.launcher.util.versioning.GameVersionNumber;

import java.time.Instant;
import java.util.List;

/**
 *
 * @author Zero
 */
@Immutable
public final class GameRemoteVersion extends RemoteVersion {

    private final ReleaseType type;

    public GameRemoteVersion(String gameVersion, String selfVersion, List<String> url, ReleaseType type, Instant releaseDate) {
        super(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId(), gameVersion, selfVersion, releaseDate, getReleaseType(type), url);
        this.type = type;
    }

    public ReleaseType getType() {
        return type;
    }

    @Override
    public Task<Version> getInstallTask(DefaultDependencyManager dependencyManager, Version baseVersion) {
        return new GameInstallTask(dependencyManager, baseVersion, this);
    }

    @Override
    public int compareTo(RemoteVersion o) {
        if (!(o instanceof GameRemoteVersion)) {
            return 0;
        }

        int dateCompare = o.getReleaseDate().compareTo(getReleaseDate());
        if (dateCompare != 0) {
            return dateCompare;
        }

        return GameVersionNumber.compare(o.getSelfVersion(), getSelfVersion());
    }

    private static Type getReleaseType(ReleaseType type) {
        if (type == null) return Type.UNCATEGORIZED;
        return switch (type) {
            case RELEASE -> Type.RELEASE;
            case SNAPSHOT -> Type.SNAPSHOT;
            case UNKNOWN -> Type.UNCATEGORIZED;
            case PENDING -> Type.PENDING;
            case UNOBFUSCATED -> Type.UNOBFUSCATED;
            default -> Type.OLD;
        };
    }
}
