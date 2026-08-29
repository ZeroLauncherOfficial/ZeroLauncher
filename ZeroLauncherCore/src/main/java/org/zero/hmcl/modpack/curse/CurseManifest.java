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
package org.zero.hmcl.modpack.curse;

import com.google.gson.annotations.SerializedName;
import org.zero.hmcl.modpack.ModpackManifest;
import org.zero.hmcl.modpack.ModpackProvider;
import org.zero.hmcl.util.gson.JsonSerializable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// @author Zero
@JsonSerializable
public record CurseManifest(@SerializedName("manifestType") String manifestType,
                            @SerializedName("manifestVersion") int manifestVersion,
                            @SerializedName("name") String name,
                            @SerializedName("version") String version,
                            @SerializedName("author") String author,
                            @SerializedName("overrides") String overrides,
                            @SerializedName("minecraft") CurseManifestMinecraft minecraft,
                            @SerializedName("files") @Unmodifiable List<CurseManifestFile> files) implements ModpackManifest {

    public CurseManifest setFiles(List<CurseManifestFile> files) {
        return new CurseManifest(manifestType, manifestVersion, name, version, author, overrides, minecraft, files);
    }

    @Override
    public ModpackProvider getProvider() {
        return CurseModpackProvider.INSTANCE;
    }

    public static final String MINECRAFT_MODPACK = "minecraftModpack";
}
