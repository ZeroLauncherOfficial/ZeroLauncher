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
package org.zero.launcher.util.tree;

import kala.compress.archivers.tar.TarArchiveEntry;
import kala.compress.archivers.tar.TarArchiveReader;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.zip.GZIPInputStream;

/// Archive file tree implementation for reading `.tar` and `.tar.gz` archives.
@NotNullByDefault
public final class TarFileTree extends ArchiveFileTree<TarArchiveReader, TarArchiveEntry> {

    /// Opens a TAR or TAR.GZ file.
    public static TarFileTree open(Path file) throws IOException {
        String fileName = file.getFileName().toString();

        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            Path tempFile = Files.createTempFile("zero-", ".tar");
            try {
                try (GZIPInputStream input = new GZIPInputStream(Files.newInputStream(file));
                     OutputStream output = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                    input.transferTo(output);
                }
                TarArchiveReader tarFile = new TarArchiveReader(tempFile);
                return new TarFileTree(tarFile, tempFile);
            } catch (Throwable e) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Throwable e2) {
                    e.addSuppressed(e2);
                }
                throw e;
            }
        } else {
            return new TarFileTree(new TarArchiveReader(file), null);
        }
    }

    private final @Nullable Path tempFile;
    private final @Nullable Thread shutdownHook;

    public TarFileTree(TarArchiveReader file, @Nullable Path tempFile) throws IOException {
        super(file);
        this.tempFile = tempFile;
        try {
            for (TarArchiveEntry entry : file.getEntries()) {
                addEntry(entry);
            }
        } catch (Throwable e) {
            try {
                file.close();
            } catch (Throwable e2) {
                e.addSuppressed(e2);
            }

            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Throwable e2) {
                    e.addSuppressed(e2);
                }
            }

            throw e;
        }

        if (tempFile != null) {
            this.shutdownHook = new Thread(() -> {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Throwable ignored) {
                }
            });
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } else {
            this.shutdownHook = null;
        }
    }

    @Override
    protected void copyAttributes(TarArchiveEntry source, Path targetFile) throws IOException {
        var fileAttributeView = Files.getFileAttributeView(targetFile, BasicFileAttributeView.class);
        if (fileAttributeView == null)
            return;

        fileAttributeView.setTimes(
                source.getLastModifiedTime(),
                source.getLastAccessTime(),
                source.getCreationTime()
        );
    }

    @Override
    public InputStream getInputStream(TarArchiveEntry entry) throws IOException {
        return reader.getInputStream(entry);
    }

    @Override
    public boolean isLink(TarArchiveEntry entry) {
        return entry.isSymbolicLink();
    }

    @Override
    public String getLink(TarArchiveEntry entry) throws IOException {
        return entry.getLinkName();
    }

    @Override
    public boolean isExecutable(TarArchiveEntry entry) {
        return entry.isFile() && (entry.getMode() & 0b1000000) != 0;
    }

    @Override
    public void close() throws IOException {
        try {
            reader.close();
        } finally {
            if (tempFile != null) {
                if (shutdownHook != null) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
