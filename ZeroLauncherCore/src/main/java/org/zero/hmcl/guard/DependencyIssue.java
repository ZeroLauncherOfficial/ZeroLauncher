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
package org.zero.hmcl.guard;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NotNullByDefault
public final class DependencyIssue {

    public enum Level {
        ERROR,   // Missing required dependency / conflict / incompatible
        WARNING  // Missing optional dependency / recommendation
    }

    public enum Type {
        MISSING_REQUIRED_DEPENDENCY,
        MISSING_OPTIONAL_DEPENDENCY,
        VERSION_INCOMPATIBLE,
        MOD_CONFLICT,
        LOADER_MISMATCH,
        GAME_VERSION_MISMATCH
    }

    private final Level level;
    private final Type type;
    private final String sourceModId;
    private final String sourceModName;
    private final String sourceModVersion;
    private final String targetModId;
    private final String targetModName;
    private final String requiredVersionRange;
    private final @Nullable String currentInstalledVersion;
    private final String description;
    private boolean selectedForInstall;

    public DependencyIssue(
            Level level,
            Type type,
            String sourceModId,
            String sourceModName,
            String sourceModVersion,
            String targetModId,
            String targetModName,
            String requiredVersionRange,
            @Nullable String currentInstalledVersion,
            String description) {
        this.level = Objects.requireNonNull(level);
        this.type = Objects.requireNonNull(type);
        this.sourceModId = Objects.requireNonNull(sourceModId);
        this.sourceModName = Objects.requireNonNull(sourceModName);
        this.sourceModVersion = Objects.requireNonNull(sourceModVersion);
        this.targetModId = Objects.requireNonNull(targetModId);
        this.targetModName = Objects.requireNonNull(targetModName);
        this.requiredVersionRange = Objects.requireNonNull(requiredVersionRange);
        this.currentInstalledVersion = currentInstalledVersion;
        this.description = Objects.requireNonNull(description);
        this.selectedForInstall = (level == Level.ERROR);
    }

    public Level getLevel() {
        return level;
    }

    public Type getType() {
        return type;
    }

    public String getSourceModId() {
        return sourceModId;
    }

    public String getSourceModName() {
        return sourceModName;
    }

    public String getSourceModVersion() {
        return sourceModVersion;
    }

    public String getTargetModId() {
        return targetModId;
    }

    public String getTargetModName() {
        return targetModName;
    }

    public String getRequiredVersionRange() {
        return requiredVersionRange;
    }

    public @Nullable String getCurrentInstalledVersion() {
        return currentInstalledVersion;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSelectedForInstall() {
        return selectedForInstall;
    }

    public void setSelectedForInstall(boolean selectedForInstall) {
        this.selectedForInstall = selectedForInstall;
    }

    public boolean isInstallable() {
        return type == Type.MISSING_REQUIRED_DEPENDENCY || type == Type.MISSING_OPTIONAL_DEPENDENCY;
    }
}