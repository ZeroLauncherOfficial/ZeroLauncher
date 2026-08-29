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
package org.zero.launcher;

import org.zero.launcher.util.StringUtils;
import org.zero.launcher.util.io.JarUtils;
import org.zero.launcher.util.platform.Architecture;
import org.zero.launcher.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Stores metadata about this application.
 */
public final class Metadata {
    private Metadata() {
    }

    public static final String NAME = "ZeroLauncher";
    public static final String FULL_NAME = "ZeroLauncher";
    public static final String VERSION = "1.0.0";

    public static final String TITLE = NAME + " v" + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    public static final int MINIMUM_REQUIRED_JAVA_VERSION = 17;
    public static final int MINIMUM_SUPPORTED_JAVA_VERSION = 17;
    public static final int RECOMMENDED_JAVA_VERSION = 21;

    public static final String PUBLISH_URL = "https://github.com/ZeroLauncherOfficial/ZeroLauncher";
    public static final String DOWNLOAD_URL = "";
    public static final String ZeroLauncher_UPDATE_URL = "";
    public static final String MANUAL_UPDATE_URL = "";

    public static final String DOCS_URL = "";
    public static final String CONTACT_URL = "";
    public static final String CHANGELOG_URL = "";
    public static final String EULA_URL = "";
    public static final String GROUPS_URL = "";

    public static final String BUILD_CHANNEL = JarUtils.getAttribute("zero.version.type", JarUtils.getAttribute("zero.version.type", "nightly"));
    public static final String GITHUB_SHA = JarUtils.getAttribute("zero.version.hash", JarUtils.getAttribute("zero.version.hash", null));

    public static final Path CURRENT_DIRECTORY = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path ZeroLauncher_USER_HOME;
    public static final Path ZeroLauncher_LOCAL_HOME;
    public static final Path DEPENDENCIES_DIRECTORY;

    static {
        String zeroHome = System.getProperty("zero.home", System.getProperty("zero.home",
                System.getenv("ZERO_USER_HOME") != null ? System.getenv("ZERO_USER_HOME") : System.getenv("ZeroLauncher_USER_HOME")));
        if (StringUtils.isBlank(zeroHome)) {
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                Path targetDir = StringUtils.isNotBlank(xdgData)
                        ? Path.of(xdgData, "zero").toAbsolutePath().normalize()
                        : Path.of(System.getProperty("user.home"), ".local", "share", "zero").toAbsolutePath().normalize();
                Path legacyDir = StringUtils.isNotBlank(xdgData)
                        ? Path.of(xdgData, "zero").toAbsolutePath().normalize()
                        : Path.of(System.getProperty("user.home"), ".local", "share", "zero").toAbsolutePath().normalize();
                if (!java.nio.file.Files.exists(targetDir) && java.nio.file.Files.exists(legacyDir)) {
                    try {
                        org.zero.launcher.util.io.FileUtils.copyDirectory(legacyDir, targetDir);
                    } catch (Throwable ignored) {
                    }
                }
                ZeroLauncher_USER_HOME = targetDir;
            } else {
                Path targetDir = OperatingSystem.getWorkingDirectory("zero");
                Path legacyDir = OperatingSystem.getWorkingDirectory("zero");
                if (!java.nio.file.Files.exists(targetDir) && java.nio.file.Files.exists(legacyDir)) {
                    try {
                        org.zero.launcher.util.io.FileUtils.copyDirectory(legacyDir, targetDir);
                    } catch (Throwable ignored) {
                    }
                }
                ZeroLauncher_USER_HOME = targetDir;
            }
        } else {
            ZeroLauncher_USER_HOME = Path.of(zeroHome).toAbsolutePath().normalize();
        }

        String zeroCurrentDir = System.getProperty("zero.dir", System.getProperty("zero.dir",
                System.getenv("ZERO_LOCAL_HOME") != null ? System.getenv("ZERO_LOCAL_HOME") : System.getenv("ZeroLauncher_LOCAL_HOME")));
        if (StringUtils.isNotBlank(zeroCurrentDir)) {
            ZeroLauncher_LOCAL_HOME = Path.of(zeroCurrentDir).toAbsolutePath().normalize();
        } else {
            Path targetDir = CURRENT_DIRECTORY.resolve(".zero");
            Path legacyDir = CURRENT_DIRECTORY.resolve(".zero");
            if (!java.nio.file.Files.exists(targetDir) && java.nio.file.Files.exists(legacyDir)) {
                try {
                    org.zero.launcher.util.io.FileUtils.copyDirectory(legacyDir, targetDir);
                } catch (Throwable ignored) {
                }
            }
            ZeroLauncher_LOCAL_HOME = targetDir;
        }

        String zeroDependencies = System.getProperty("zero.dependencies.dir", System.getProperty("zero.dependencies.dir",
                System.getenv("ZERO_DEPENDENCIES_DIR") != null ? System.getenv("ZERO_DEPENDENCIES_DIR") : System.getenv("ZeroLauncher_DEPENDENCIES_DIR")));
        DEPENDENCIES_DIRECTORY = StringUtils.isNotBlank(zeroDependencies)
                ? Path.of(zeroDependencies).toAbsolutePath().normalize()
                : ZeroLauncher_LOCAL_HOME.resolve("dependencies");
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }

    public static @Nullable String getSuggestedJavaDownloadLink() {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX && Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64_OW)
            return "https://www.loongnix.cn/zh/api/java/downloads-jdk21/index.html";
        else {
            EnumSet<Architecture> supportedArchitectures;
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.X86, Architecture.ARM64);
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX)
                supportedArchitectures = EnumSet.of(
                        Architecture.X86_64, Architecture.X86,
                        Architecture.ARM64, Architecture.ARM32,
                        Architecture.RISCV64, Architecture.LOONGARCH64
                );
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.ARM64);
            else
                supportedArchitectures = EnumSet.noneOf(Architecture.class);
            if (supportedArchitectures.contains(Architecture.SYSTEM_ARCH))
                return String.format("https://docs.zero.net/downloads/%s/%s.html",
                        OperatingSystem.CURRENT_OS.getCheckedName(),
                        Architecture.SYSTEM_ARCH.getCheckedName()
                );
            else
                return null;
        }
    }
}
