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
package org.zero.hmcl.download.game;

import org.zero.hmcl.game.DefaultGameRepository;
import org.zero.hmcl.game.Version;
import org.zero.hmcl.task.Task;
import org.zero.hmcl.util.gson.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This task is to save the version json.
 *
 * @author Zero
 */
public final class VersionJsonSaveTask extends Task<Version> {

    private final DefaultGameRepository repository;
    private final Version version;

    /**
     * Constructor.
     *
     * @param repository the game repository
     * @param version the game version
     */
    public VersionJsonSaveTask(DefaultGameRepository repository, Version version) {
        this.repository = repository;
        this.version = version;

        setSignificance(TaskSignificance.MODERATE);
        setResult(version);
    }

    @Override
    public void execute() throws Exception {
        Path json = repository.getVersionJson(version.getId()).toAbsolutePath();
        Files.createDirectories(json.getParent());
        JsonUtils.writeToJsonFile(json, version);
    }
}
