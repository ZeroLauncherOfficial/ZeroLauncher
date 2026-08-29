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
package org.zero.hmcl.download.legacyfabric;

import org.zero.hmcl.download.DownloadProvider;
import org.zero.hmcl.download.VersionList;
import org.zero.hmcl.addon.RemoteAddon;
import org.zero.hmcl.addon.repository.ModrinthRemoteAddonRepository;
import org.zero.hmcl.task.Task;
import org.zero.hmcl.util.Lang;

import java.util.Collections;

public class LegacyFabricAPIVersionList extends VersionList<LegacyFabricAPIRemoteVersion> {

    private final DownloadProvider downloadProvider;

    public LegacyFabricAPIVersionList(DownloadProvider downloadProvider) {
        this.downloadProvider = downloadProvider;
    }

    @Override
    public boolean hasType() {
        return false;
    }

    @Override
    public Task<?> refreshAsync() {
        return Task.runAsync(() -> {
            for (RemoteAddon.Version modVersion : Lang.toIterable(ModrinthRemoteAddonRepository.MODS.getRemoteVersionsById(downloadProvider, "legacy-fabric-api"))) {
                for (String gameVersion : modVersion.gameVersions()) {
                    versions.put(gameVersion, new LegacyFabricAPIRemoteVersion(gameVersion, modVersion.version(), modVersion.name(), modVersion.datePublished(), modVersion,
                            Collections.singletonList(modVersion.file().url())));
                }
            }
        });
    }
}
