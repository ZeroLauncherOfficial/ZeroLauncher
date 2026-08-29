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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NotNullByDefault
public final class ZeroGuardReport {

    private final String instanceId;
    private final String gameVersion;
    private final String loaderType;
    private final int totalModsScanned;
    private final List<DependencyIssue> issues;

    public ZeroGuardReport(String instanceId, String gameVersion, String loaderType, int totalModsScanned, List<DependencyIssue> issues) {
        this.instanceId = Objects.requireNonNull(instanceId);
        this.gameVersion = Objects.requireNonNull(gameVersion);
        this.loaderType = Objects.requireNonNull(loaderType);
        this.totalModsScanned = totalModsScanned;
        this.issues = Collections.unmodifiableList(Objects.requireNonNull(issues));
    }

    public static ZeroGuardReport clean(String instanceId, String gameVersion, String loaderType, int totalModsScanned) {
        return new ZeroGuardReport(instanceId, gameVersion, loaderType, totalModsScanned, Collections.emptyList());
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public String getLoaderType() {
        return loaderType;
    }

    public int getTotalModsScanned() {
        return totalModsScanned;
    }

    public List<DependencyIssue> getIssues() {
        return issues;
    }

    public boolean isClean() {
        return issues.isEmpty();
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(i -> i.getLevel() == DependencyIssue.Level.ERROR);
    }

    public boolean hasWarnings() {
        return issues.stream().anyMatch(i -> i.getLevel() == DependencyIssue.Level.WARNING);
    }

    public int getErrorCount() {
        return (int) issues.stream().filter(i -> i.getLevel() == DependencyIssue.Level.ERROR).count();
    }

    public int getWarningCount() {
        return (int) issues.stream().filter(i -> i.getLevel() == DependencyIssue.Level.WARNING).count();
    }
}