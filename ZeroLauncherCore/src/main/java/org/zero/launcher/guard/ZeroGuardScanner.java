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
package org.zero.launcher.guard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kala.compress.archivers.zip.ZipArchiveEntry;
import org.zero.launcher.addon.mod.ModLoaderType;
import org.zero.launcher.download.LibraryAnalyzer;
import org.zero.launcher.game.GameRepository;
import org.zero.launcher.game.Version;
import org.zero.launcher.util.io.CompressingUtils;
import org.zero.launcher.util.logging.Logger;
import org.zero.launcher.util.tree.ZipFileTree;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@NotNullByDefault
public final class ZeroGuardScanner {

    public static final class ScannedMod {
        public final Path filePath;
        public final String modId;
        public final String name;
        public final String version;
        public final Set<String> providedIds = new HashSet<>();
        public final List<RawDependency> dependencies = new ArrayList<>();
        public final List<RawDependency> breaks = new ArrayList<>();

        public ScannedMod(Path filePath, String modId, String name, String version) {
            this.filePath = filePath;
            this.modId = modId.toLowerCase(Locale.ROOT);
            this.name = (name == null || name.isBlank()) ? modId : name;
            this.version = (version == null || version.isBlank()) ? "1.0.0" : version;
            this.providedIds.add(this.modId);
        }
    }

    public static final class RawDependency {
        public final String targetModId;
        public final String versionRange;
        public final boolean mandatory;

        public RawDependency(String targetModId, String versionRange, boolean mandatory) {
            this.targetModId = targetModId.toLowerCase(Locale.ROOT);
            this.versionRange = versionRange == null ? "*" : versionRange;
            this.mandatory = mandatory;
        }
    }

    public static ZeroGuardReport scan(GameRepository repository, String versionId) {
        long startTime = System.currentTimeMillis();
        String gameVersion = "";
        String loaderType = "Vanilla";

        try {
            gameVersion = repository.getGameVersion(versionId).orElse("");
            Version resolved = repository.getResolvedVersion(versionId);
            if (resolved != null) {
                LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(resolved, gameVersion);
                Set<ModLoaderType> loaders = analyzer.getModLoaders();
                if (!loaders.isEmpty()) {
                    loaderType = loaders.iterator().next().name();
                }
            }
        } catch (Throwable ignored) {
        }

        Path modsDir = repository.getModsDirectory(versionId);
        if (!Files.isDirectory(modsDir)) {
            return ZeroGuardReport.clean(versionId, gameVersion, loaderType, 0);
        }

        List<ScannedMod> mods = new ArrayList<>();
        Map<String, ScannedMod> installedModMap = new HashMap<>();
        Set<String> allProvidedIds = new HashSet<>();

        allProvidedIds.add("minecraft");
        allProvidedIds.add("java");
        allProvidedIds.add("fabricloader");
        allProvidedIds.add("fabric");
        allProvidedIds.add("forge");
        allProvidedIds.add("neoforge");
        allProvidedIds.add("quilt_loader");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (Files.isRegularFile(file) && (fileName.endsWith(".jar") || fileName.endsWith(".litemod")) && !fileName.endsWith(".disabled")) {
                    try {
                        ScannedMod scanned = parseModFile(file);
                        if (scanned != null) {
                            mods.add(scanned);
                            installedModMap.put(scanned.modId, scanned);
                            allProvidedIds.addAll(scanned.providedIds);
                        }
                    } catch (Throwable e) {
                        Logger.LOG.warning("ZeroGuard: Failed to parse mod file " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Throwable e) {
            Logger.LOG.warning("ZeroGuard: Error scanning mods directory: " + e.getMessage());
        }

        List<DependencyIssue> issues = new ArrayList<>();

        for (ScannedMod mod : mods) {
            for (RawDependency dep : mod.dependencies) {
                String targetId = dep.targetModId;

                if ("minecraft".equals(targetId)) {
                    continue;
                }

                if ("fabricloader".equals(targetId) || "fabric".equals(targetId)) {
                    if (!loaderType.toLowerCase(Locale.ROOT).contains("fabric") && !loaderType.toLowerCase(Locale.ROOT).contains("quilt")) {
                        issues.add(new DependencyIssue(
                                DependencyIssue.Level.ERROR,
                                DependencyIssue.Type.LOADER_MISMATCH,
                                mod.modId, mod.name, mod.version,
                                targetId, "Fabric Loader", dep.versionRange, loaderType,
                                "此 Mod 需要 Fabric Loader 運行環境，當前實例 Loader 為 " + loaderType
                        ));
                    }
                    continue;
                }

                if ("forge".equals(targetId)) {
                    if (!loaderType.toLowerCase(Locale.ROOT).contains("forge") && !loaderType.toLowerCase(Locale.ROOT).contains("neoforge")) {
                        issues.add(new DependencyIssue(
                                DependencyIssue.Level.ERROR,
                                DependencyIssue.Type.LOADER_MISMATCH,
                                mod.modId, mod.name, mod.version,
                                targetId, "Forge Loader", dep.versionRange, loaderType,
                                "此 Mod 需要 Forge / NeoForge 運行環境，當前實例 Loader 為 " + loaderType
                        ));
                    }
                    continue;
                }

                if ("neoforge".equals(targetId)) {
                    if (!loaderType.toLowerCase(Locale.ROOT).contains("neoforge")) {
                        issues.add(new DependencyIssue(
                                DependencyIssue.Level.ERROR,
                                DependencyIssue.Type.LOADER_MISMATCH,
                                mod.modId, mod.name, mod.version,
                                targetId, "NeoForge Loader", dep.versionRange, loaderType,
                                "此 Mod 需要 NeoForge 運行環境，當前實例 Loader 為 " + loaderType
                        ));
                    }
                    continue;
                }

                if ("java".equals(targetId)) {
                    continue;
                }

                if (!allProvidedIds.contains(targetId)) {
                    String displayName = formatModDisplayName(targetId);
                    if (dep.mandatory) {
                        issues.add(new DependencyIssue(
                                DependencyIssue.Level.ERROR,
                                DependencyIssue.Type.MISSING_REQUIRED_DEPENDENCY,
                                mod.modId, mod.name, mod.version,
                                targetId, displayName, dep.versionRange, null,
                                "缺少必要前置模組 " + displayName + " (" + dep.versionRange + ")，未安裝可能導致遊戲崩潰"
                        ));
                    } else {
                        issues.add(new DependencyIssue(
                                DependencyIssue.Level.WARNING,
                                DependencyIssue.Type.MISSING_OPTIONAL_DEPENDENCY,
                                mod.modId, mod.name, mod.version,
                                targetId, displayName, dep.versionRange, null,
                                "缺少建議可選前置 " + displayName + " (" + dep.versionRange + ")，建議安裝以解鎖完整功能"
                        ));
                    }
                }
            }

            for (RawDependency brk : mod.breaks) {
                if (allProvidedIds.contains(brk.targetModId)) {
                    ScannedMod conflictMod = installedModMap.get(brk.targetModId);
                    String conflictName = conflictMod != null ? conflictMod.name : brk.targetModId;
                    issues.add(new DependencyIssue(
                            DependencyIssue.Level.ERROR,
                            DependencyIssue.Type.MOD_CONFLICT,
                            mod.modId, mod.name, mod.version,
                            brk.targetModId, conflictName, brk.versionRange, conflictMod != null ? conflictMod.version : "",
                            "與已安裝的模組 " + conflictName + " 存在已知衝突，同時運行將導致遊戲崩潰"
                    ));
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        Logger.LOG.info("ZeroGuard: Scanned " + mods.size() + " mods in " + elapsed + "ms. Found " + issues.size() + " issues.");

        return new ZeroGuardReport(versionId, gameVersion, loaderType, mods.size(), issues);
    }

    private static @Nullable ScannedMod parseModFile(Path file) {
        try (ZipFileTree tree = CompressingUtils.openZipTree(file)) {
            ZipArchiveEntry fabricEntry = tree.getEntry("fabric.mod.json");
            if (fabricEntry != null) {
                try (InputStream is = tree.getInputStream(fabricEntry)) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                    String id = json.has("id") ? json.get("id").getAsString() : "";
                    String name = json.has("name") ? json.get("name").getAsString() : id;
                    String version = json.has("version") ? json.get("version").getAsString() : "1.0.0";
                    if (!id.isBlank()) {
                        ScannedMod mod = new ScannedMod(file, id, name, version);
                        extractFabricDeps(json, "depends", mod.dependencies, true);
                        extractFabricDeps(json, "recommends", mod.dependencies, false);
                        extractFabricDeps(json, "suggests", mod.dependencies, false);
                        extractFabricDeps(json, "breaks", mod.breaks, true);
                        extractFabricDeps(json, "conflicts", mod.breaks, true);

                        if (json.has("provides") && json.get("provides").isJsonArray()) {
                            for (JsonElement elem : json.getAsJsonArray("provides")) {
                                if (elem.isJsonPrimitive()) {
                                    mod.providedIds.add(elem.getAsString().toLowerCase(Locale.ROOT));
                                }
                            }
                        }
                        return mod;
                    }
                }
            }

            ZipArchiveEntry tomlEntry = tree.getEntry("META-INF/mods.toml");
            if (tomlEntry == null) {
                tomlEntry = tree.getEntry("META-INF/neoforge.mods.toml");
            }
            if (tomlEntry != null) {
                try (InputStream is = tree.getInputStream(tomlEntry)) {
                    TomlParseResult toml = Toml.parse(is);
                    TomlArray modsArray = toml.getArray("mods");
                    if (modsArray != null && !modsArray.isEmpty()) {
                        TomlTable firstMod = modsArray.getTable(0);
                        String modId = firstMod.getString("modId");
                        String displayName = firstMod.getString("displayName");
                        String version = firstMod.getString("version");
                        if (modId != null && !modId.isBlank()) {
                            ScannedMod mod = new ScannedMod(file, modId, displayName != null ? displayName : modId, version != null ? version : "1.0.0");

                            TomlTable dependenciesTable = toml.getTable("dependencies");
                            if (dependenciesTable != null) {
                                for (String key : dependenciesTable.keySet()) {
                                    TomlArray depArray = dependenciesTable.getArray(key);
                                    if (depArray != null) {
                                        for (int i = 0; i < depArray.size(); i++) {
                                            TomlTable dep = depArray.getTable(i);
                                            String depModId = dep.getString("modId");
                                            Boolean mandatory = dep.getBoolean("mandatory");
                                            String versionRange = dep.getString("versionRange");
                                            if (depModId != null && !depModId.isBlank()) {
                                                mod.dependencies.add(new RawDependency(depModId, versionRange, mandatory != null ? mandatory : true));
                                            }
                                        }
                                    }
                                }
                            }
                            return mod;
                        }
                    }
                }
            }

            ZipArchiveEntry quiltEntry = tree.getEntry("quilt.mod.json");
            if (quiltEntry != null) {
                try (InputStream is = tree.getInputStream(quiltEntry)) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (json.has("quilt_loader")) {
                        JsonObject ql = json.getAsJsonObject("quilt_loader");
                        String id = ql.has("id") ? ql.get("id").getAsString() : "";
                        JsonObject meta = ql.has("metadata") ? ql.getAsJsonObject("metadata") : null;
                        String name = (meta != null && meta.has("name")) ? meta.get("name").getAsString() : id;
                        String version = ql.has("version") ? ql.get("version").getAsString() : "1.0.0";
                        if (!id.isBlank()) {
                            ScannedMod mod = new ScannedMod(file, id, name, version);
                            return mod;
                        }
                    }
                }
            }

            ZipArchiveEntry mcmodEntry = tree.getEntry("mcmod.info");
            if (mcmodEntry != null) {
                try (InputStream is = tree.getInputStream(mcmodEntry)) {
                    JsonElement parsed = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    JsonArray array = parsed.isJsonArray() ? parsed.getAsJsonArray() :
                            (parsed.isJsonObject() && parsed.getAsJsonObject().has("modList") ? parsed.getAsJsonObject().getAsJsonArray("modList") : null);
                    if (array != null && !array.isEmpty()) {
                        JsonObject obj = array.get(0).getAsJsonObject();
                        String modid = obj.has("modid") ? obj.get("modid").getAsString() : "";
                        String name = obj.has("name") ? obj.get("name").getAsString() : modid;
                        String version = obj.has("version") ? obj.get("version").getAsString() : "1.0.0";
                        if (!modid.isBlank()) {
                            return new ScannedMod(file, modid, name, version);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void extractFabricDeps(JsonObject json, String key, List<RawDependency> targetList, boolean mandatory) {
        if (!json.has(key)) return;
        JsonElement elem = json.get(key);
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String depId = entry.getKey();
                String versionRange = "*";
                if (entry.getValue().isJsonPrimitive()) {
                    versionRange = entry.getValue().getAsString();
                } else if (entry.getValue().isJsonArray()) {
                    versionRange = entry.getValue().toString();
                }
                targetList.add(new RawDependency(depId, versionRange, mandatory));
            }
        }
    }

    private static String formatModDisplayName(String modId) {
        String lower = modId.toLowerCase(Locale.ROOT);
        if (lower.equals("fabric-api") || lower.equals("fabric")) return "Fabric API";
        if (lower.equals("cloth-config") || lower.equals("cloth-config2") || lower.equals("cloth_config")) return "Cloth Config";
        if (lower.equals("architectury") || lower.equals("architectury-api")) return "Architectury API";
        if (lower.equals("forge")) return "Minecraft Forge";
        if (lower.equals("neoforge")) return "NeoForge";
        if (lower.equals("fabricloader")) return "Fabric Loader";
        if (lower.equals("quilt_loader")) return "Quilt Loader";
        if (lower.equals("jei")) return "Just Enough Items";
        if (lower.equals("rei") || lower.equals("roughlyenoughitems")) return "Roughly Enough Items";
        if (lower.equals("emi")) return "EMI";
        if (lower.equals("sodium")) return "Sodium";
        if (lower.equals("iris")) return "Iris Shaders";
        if (lower.equals("indium")) return "Indium";
        if (lower.equals("modmenu")) return "Mod Menu";
        if (lower.equals("geckolib")) return "GeckoLib";
        if (lower.equals("citresewn")) return "CIT Resewn";
        if (lower.equals("curios")) return "Curios API";
        if (lower.equals("trinkets")) return "Trinkets";
        if (lower.equals("appleskin")) return "AppleSkin";
        return modId;
    }
}
