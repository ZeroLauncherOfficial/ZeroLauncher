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
package org.zero.launcher.addon.meta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import kala.compress.archivers.zip.ZipArchiveEntry;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.zero.launcher.addon.LocalAddonFile;
import org.zero.launcher.addon.mod.LocalModFile;
import org.zero.launcher.addon.mod.ModLoaderType;
import org.zero.launcher.addon.mod.ModManager;
import org.zero.launcher.util.Immutable;
import org.zero.launcher.util.gson.JsonUtils;
import org.zero.launcher.util.tree.ZipFileTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

/// Metadata model for Quilt mods parsed from quilt.mod.json.
@Immutable
@NotNullByDefault
public final class QuiltModMetadata {
    private static final class QuiltLoader {
        private static final class Metadata {
            private final String name;
            private final String description;
            private final JsonObject contributors;
            private final String icon;
            private final JsonObject contact;

            public Metadata(String name, String description, JsonObject contributors, String icon, JsonObject contact) {
                this.name = name;
                this.description = description;
                this.contributors = contributors;
                this.icon = icon;
                this.contact = contact;
            }
        }

        private final String id;
        private final String version;
        private final Metadata metadata;

        public QuiltLoader(String id, String version, Metadata metadata) {
            this.id = id;
            this.version = version;
            this.metadata = metadata;
        }
    }

    private final int schema_version;
    private final QuiltLoader quilt_loader;

    public QuiltModMetadata(int schemaVersion, QuiltLoader quiltLoader) {
        this.schema_version = schemaVersion;
        this.quilt_loader = quiltLoader;
    }

    public static LocalModFile fromFile(ModManager modManager, Path modFile, ZipFileTree tree) throws IOException, JsonParseException {
        ZipArchiveEntry path = tree.getEntry("quilt.mod.json");
        if (path == null) {
            throw new IOException("File " + modFile + " is not a Quilt mod.");
        }

        QuiltModMetadata root = JsonUtils.fromNonNullJsonFully(tree.getInputStream(path), QuiltModMetadata.class);
        if (root.schema_version != 1) {
            throw new IOException("File " + modFile + " is not a supported Quilt mod.");
        }

        String id = root.quilt_loader != null && root.quilt_loader.id != null ? root.quilt_loader.id : "";
        String version = root.quilt_loader != null && root.quilt_loader.version != null ? root.quilt_loader.version : "";
        QuiltLoader.@Nullable Metadata meta = root.quilt_loader != null ? root.quilt_loader.metadata : null;
        String name = meta != null && meta.name != null ? meta.name : id;
        String description = meta != null && meta.description != null ? meta.description : "";
        @Nullable String icon = meta != null ? meta.icon : null;

        String authors = "";
        if (meta != null && meta.contributors != null) {
            authors = meta.contributors.entrySet().stream()
                    .map(entry -> {
                        String role = entry.getValue() != null && entry.getValue().isJsonPrimitive()
                                ? entry.getValue().getAsString() : "";
                        return role.isEmpty() ? entry.getKey() : String.format("%s (%s)", entry.getKey(), role);
                    })
                    .collect(Collectors.joining(", "));
        }

        String homepage = "";
        if (meta != null && meta.contact != null && meta.contact.has("homepage")) {
            var el = meta.contact.get("homepage");
            if (el != null && el.isJsonPrimitive()) {
                homepage = el.getAsString();
            }
        }

        return new LocalModFile(
                modManager,
                modManager.getLocalMod(id, ModLoaderType.QUILT),
                modFile,
                name,
                new LocalAddonFile.Description(description),
                authors,
                version,
                "",
                homepage,
                icon
        );
    }
}
