/*
 * ZeroLauncher
 * Copyright (C) 2024 Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.launcher.java;

import kala.compress.archivers.ArchiveEntry;
import org.zero.launcher.task.Task;
import org.zero.launcher.util.DigestUtils;
import org.zero.launcher.util.io.FileUtils;
import org.zero.launcher.util.io.IOUtils;
import org.zero.launcher.util.tree.ArchiveFileTree;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.zero.launcher.util.logging.Logger.LOG;

/// Task that extracts and installs a Java runtime archive into a target directory.
@NotNullByDefault
public final class JavaInstallTask extends Task<JavaManifest> {

    /// The destination directory for the installed Java runtime.
    private final Path targetDir;

    /// Manifest update attributes.
    private final Map<String, Object> update;

    /// The path to the downloaded archive file.
    private final Path archiveFile;

    private final Map<String, JavaLocalFiles.Local> files = new LinkedHashMap<>();
    private final ArrayList<String> nameStack = new ArrayList<>();
    private final byte[] buffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
    private final MessageDigest messageDigest = DigestUtils.getDigest("SHA-1");

    /// Creates a new Java installation task.
    ///
    /// @param targetDir the target directory
    /// @param update the manifest update map
    /// @param archiveFile the archive file path
    public JavaInstallTask(Path targetDir, Map<String, Object> update, Path archiveFile) {
        this.targetDir = targetDir;
        this.update = update;
        this.archiveFile = archiveFile;
    }

    @Override
    public void execute() throws Exception {
        JavaInfo info;

        try (ArchiveFileTree<?, ?> tree = ArchiveFileTree.open(archiveFile)) {
            info = JavaInfo.fromArchive(tree);
            copyDirContent(tree, targetDir);
        }

        setResult(new JavaManifest(info, update, files));
    }

    private <F, E extends ArchiveEntry> void copyDirContent(ArchiveFileTree<F, E> tree, Path targetDir) throws IOException {
        ArchiveFileTree.Dir<E> root = JavaInfo.findJdkRoot(tree.getRoot());
        if (root == null) {
            if (!tree.getRoot().getSubDirs().isEmpty()) {
                root = tree.getRoot().getSubDirs().values().iterator().next();
            } else {
                root = tree.getRoot();
            }
        }
        copyDirContent(tree, root, targetDir);
    }

    private <F, E extends ArchiveEntry> void copyDirContent(ArchiveFileTree<F, E> tree, ArchiveFileTree.Dir<E> dir, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        for (Map.Entry<String, E> pair : dir.getFiles().entrySet()) {
            Path path = targetDir.resolve(pair.getKey());
            E entry = pair.getValue();

            nameStack.add(pair.getKey());
            if (tree.isLink(entry)) {
                String linkTarget = tree.getLink(entry);
                files.put(String.join("/", nameStack), new JavaLocalFiles.LocalLink(linkTarget));
                try {
                    Files.createSymbolicLink(path, Paths.get(linkTarget));
                } catch (FileSystemException | UnsupportedOperationException | SecurityException e) {
                    LOG.warning("Failed to create symbolic link " + path + " -> " + linkTarget + ", attempting fallback copy: " + e.getMessage());
                    Path resolvedTarget = path.getParent() != null ? path.getParent().resolve(linkTarget) : Paths.get(linkTarget);
                    if (Files.exists(resolvedTarget)) {
                        try {
                            Files.copy(resolvedTarget, path, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException copyEx) {
                            LOG.warning("Fallback copy failed for link " + path, copyEx);
                        }
                    }
                }
            } else {
                long size = 0L;

                try (InputStream input = tree.getInputStream(entry);
                     OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    messageDigest.reset();

                    int c;
                    while ((c = input.read(buffer)) > 0) {
                        size += c;
                        output.write(buffer, 0, c);
                        messageDigest.update(buffer, 0, c);
                    }
                }

                if (tree.isExecutable(entry))
                    FileUtils.setExecutable(path);

                files.put(String.join("/", nameStack), new JavaLocalFiles.LocalFile(HexFormat.of().formatHex(messageDigest.digest()), size));
            }
            nameStack.remove(nameStack.size() - 1);
        }

        for (Map.Entry<String, ArchiveFileTree.Dir<E>> pair : dir.getSubDirs().entrySet()) {
            nameStack.add(pair.getKey());
            files.put(String.join("/", nameStack), new JavaLocalFiles.LocalDirectory());
            copyDirContent(tree, pair.getValue(), targetDir.resolve(pair.getKey()));
            nameStack.remove(nameStack.size() - 1);
        }
    }
}
