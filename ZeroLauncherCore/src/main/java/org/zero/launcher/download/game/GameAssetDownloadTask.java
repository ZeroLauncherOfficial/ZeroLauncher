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
package org.zero.launcher.download.game;

import com.google.gson.JsonParseException;
import org.zero.launcher.download.AbstractDependencyManager;
import org.zero.launcher.game.AssetIndex;
import org.zero.launcher.game.AssetIndexInfo;
import org.zero.launcher.game.AssetObject;
import org.zero.launcher.game.Version;
import org.zero.launcher.task.FileDownloadTask;
import org.zero.launcher.task.Task;
import org.zero.launcher.util.CacheRepository;
import org.zero.launcher.util.gson.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.zero.launcher.util.logging.Logger.LOG;

/**
 *
 * @author Zero
 */
public final class GameAssetDownloadTask extends Task<Void> {
    
    private final AbstractDependencyManager dependencyManager;
    private final Version version;
    private final AssetIndexInfo assetIndexInfo;
    private final Path assetIndexFile;
    private final boolean integrityCheck;
    private final List<Task<?>> dependents = new ArrayList<>(1);
    private final List<Task<?>> dependencies = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param dependencyManager the dependency manager that can provides {@link org.zero.launcher.game.GameRepository}
     * @param version the game version
     */
    public GameAssetDownloadTask(AbstractDependencyManager dependencyManager, Version version, boolean forceDownloadingIndex, boolean integrityCheck) {
        this.dependencyManager = dependencyManager;
        this.version = version.resolve(dependencyManager.getGameRepository());
        this.assetIndexInfo = this.version.getAssetIndex();
        this.assetIndexFile = dependencyManager.getGameRepository().getIndexFile(version.getId(), assetIndexInfo.getId());
        this.integrityCheck = integrityCheck;

        setStage("zero.install.assets");
        dependents.add(new GameAssetIndexDownloadTask(dependencyManager, this.version, forceDownloadingIndex));
    }

    @Override
    public Collection<Task<?>> getDependents() {
        return dependents;
    }

    @Override
    public Collection<Task<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public void execute() throws Exception {
        AssetIndex index;
        try {
            index = JsonUtils.fromNonNullJson(Files.readString(assetIndexFile), AssetIndex.class);
        } catch (IOException | JsonParseException e) {
            throw new GameAssetIndexDownloadTask.GameAssetIndexMalformedException();
        }

        int progress = 0;
        for (AssetObject assetObject : index.getObjects().values()) {
            if (isCancelled())
                throw new InterruptedException();

            Path file = dependencyManager.getGameRepository().getAssetObject(version.getId(), assetIndexInfo.getId(), assetObject);
            boolean download = !Files.isRegularFile(file);
            try {
                if (!download && integrityCheck && !assetObject.validateChecksum(file, true))
                    download = true;
            } catch (IOException e) {
                LOG.warning("Unable to calc hash value of file " + file, e);
            }
            if (download) {
                List<URI> uris = dependencyManager.getDownloadProvider().getAssetObjectCandidates(assetObject.getLocation());

                var task = new FileDownloadTask(uris, file, new FileDownloadTask.IntegrityCheck("SHA-1", assetObject.hash()));
                task.setName(assetObject.hash());
                task.setCandidate(dependencyManager.getCacheRepository().getCommonDirectory()
                        .resolve("assets").resolve("objects").resolve(assetObject.getLocation()));
                task.setCacheRepository(dependencyManager.getCacheRepository());
                task.setCaching(true);
                dependencies.add(task.withCounter("zero.install.assets"));
            } else {
                dependencyManager.getCacheRepository().tryCacheFile(file, CacheRepository.SHA1, assetObject.hash());
            }

            updateProgress(++progress, index.getObjects().size());
        }

        if (!dependencies.isEmpty()) {
            getProperties().put("total", dependencies.size());
            notifyPropertiesChanged();
        }
    }

    public static final boolean DOWNLOAD_INDEX_FORCIBLY = true;
    public static final boolean DOWNLOAD_INDEX_IF_NECESSARY = false;
}
