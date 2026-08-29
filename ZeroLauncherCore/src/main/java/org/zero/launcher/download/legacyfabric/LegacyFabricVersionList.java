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

import org.zero.launcher.download.DownloadProvider;
import org.zero.launcher.download.VersionList;
import org.zero.launcher.task.Task;
import org.zero.launcher.util.gson.JsonUtils;
import org.zero.launcher.util.io.NetworkUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zero.launcher.util.gson.JsonUtils.listTypeOf;

public final class LegacyFabricVersionList extends VersionList<LegacyFabricRemoteVersion> {
    private final DownloadProvider downloadProvider;

    public LegacyFabricVersionList(DownloadProvider downloadProvider) {
        this.downloadProvider = downloadProvider;
    }

    @Override
    public boolean hasType() {
        return false;
    }

    @Override
    public Task<?> refreshAsync() {
        return Task.runAsync(() -> {
            List<String> gameVersions = getGameVersions(GAME_META_URL);
            List<String> loaderVersions = getGameVersions(LOADER_META_URL);

            lock.writeLock().lock();

            try {
                for (String metaGameVersion : gameVersions) {
                    String gameVersion = normalizeVersion(metaGameVersion);
                    for (String loaderVersion : loaderVersions) {
                        versions.put(gameVersion, new LegacyFabricRemoteVersion(gameVersion, loaderVersion,
                                Collections.singletonList(getLaunchMetaUrl(metaGameVersion, loaderVersion))));
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    private static final String LOADER_META_URL = "https://meta.legacyfabric.net/v2/versions/loader";
    private static final String GAME_META_URL = "https://meta.legacyfabric.net/v2/versions/game";

    private List<String> getGameVersions(String metaUrl) throws IOException {
        String json = NetworkUtils.doGet(downloadProvider.injectURLWithCandidates(metaUrl));
        return JsonUtils.GSON.fromJson(json, listTypeOf(GameVersion.class))
                .stream().map(GameVersion::getVersion).collect(Collectors.toList());
    }

    private static String normalizeVersion(String version) {
        return version.startsWith("2point0_")
                ? "2.0_" + version.substring("2point0_".length())
                : version;
    }

    private static String getLaunchMetaUrl(String gameVersion, String loaderVersion) {
        return String.format("https://meta.legacyfabric.net/v2/versions/loader/%s/%s", gameVersion, loaderVersion);
    }

    private static class GameVersion {
        private final String version;
        private final String maven;
        private final boolean stable;

        public GameVersion() {
            this("", null, false);
        }

        public GameVersion(String version, String maven, boolean stable) {
            this.version = version;
            this.maven = maven;
            this.stable = stable;
        }

        public String getVersion() {
            return version;
        }

        @Nullable
        public String getMaven() {
            return maven;
        }

        public boolean isStable() {
            return stable;
        }
    }
}
