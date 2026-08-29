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
package org.zero.hmcl.game;

import com.google.gson.JsonParseException;
import kala.compress.archivers.zip.ZipArchiveReader;
import org.zero.hmcl.download.DefaultDependencyManager;
import org.zero.hmcl.modpack.MismatchedModpackTypeException;
import org.zero.hmcl.modpack.Modpack;
import org.zero.hmcl.modpack.ModpackProvider;
import org.zero.hmcl.modpack.ModpackUpdateTask;
import org.zero.hmcl.task.Task;
import org.zero.hmcl.util.StringUtils;
import org.zero.hmcl.util.gson.JsonUtils;
import org.zero.hmcl.util.io.CompressingUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class ZeroLauncherModpackProvider implements ModpackProvider {
    public static final ZeroLauncherModpackProvider INSTANCE = new ZeroLauncherModpackProvider();

    @Override
    public String getName() {
        return "ZeroLauncher";
    }

    @Override
    public Task<?> createCompletionTask(DefaultDependencyManager dependencyManager, String version) {
        return null;
    }

    @Override
    public Task<?> createUpdateTask(DefaultDependencyManager dependencyManager, String name, Path zipFile, Modpack modpack) throws MismatchedModpackTypeException {
        if (!(modpack.getManifest() instanceof ZeroLauncherModpackManifest))
            throw new MismatchedModpackTypeException(getName(), modpack.getManifest().getProvider().getName());

        if (!(dependencyManager.getGameRepository() instanceof ZeroLauncherGameRepository repository)) {
            throw new IllegalArgumentException("ZeroLauncherModpackProvider requires ZeroLauncherGameRepository");
        }

        return new ModpackUpdateTask(dependencyManager.getGameRepository(), name, new ZeroLauncherModpackInstallTask(repository, zipFile, modpack, name));
    }

    @Override
    public Modpack readManifest(ZipArchiveReader file, Path path, Charset encoding) throws IOException, JsonParseException {
        String manifestJson = CompressingUtils.readTextZipEntry(file, "modpack.json");
        Modpack manifest = JsonUtils.fromNonNullJson(manifestJson, ZeroLauncherModpack.class).setEncoding(encoding);
        String gameJson = CompressingUtils.readTextZipEntry(file, "minecraft/pack.json");
        Version game = JsonUtils.fromNonNullJson(gameJson, Version.class);
        if (game.getJar() == null)
            if (StringUtils.isBlank(manifest.getVersion()))
                throw new JsonParseException("Cannot recognize the game version of modpack " + file + ".");
            else
                manifest.setManifest(ZeroLauncherModpackManifest.INSTANCE);
        else
            manifest.setManifest(ZeroLauncherModpackManifest.INSTANCE).setGameVersion(game.getJar());
        return manifest;
    }

    private final static class ZeroLauncherModpack extends Modpack {
        @Override
        public Task<?> getInstallTask(DefaultDependencyManager dependencyManager, Path zipFile, String name, String iconUrl) {
            return new ZeroLauncherModpackInstallTask((ZeroLauncherGameRepository) dependencyManager.getGameRepository(), zipFile, this, name);
        }
    }

}
