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
package org.zero.launcher.util.i18n;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.zero.launcher.Metadata;
import org.zero.launcher.util.gson.JsonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.zero.launcher.util.logging.Logger.LOG;

/**
 * <h1>LanguagePackManager — 語言包與詞條覆蓋管理器</h1>
 * <p>
 * 支援「寫什麼 替換什麼 (Partial Key-Value Overlay)」模式。
 * 內建 5 套覆蓋全啟動器 1600+ 詞條的官方特色語言包（包含喵喵物語語言包），
 * 同時支援玩家自訂單一或多個詞條，未覆蓋的詞條自動向下繼承預設語言檔。
 *
 * @author Zero
 */
@NotNullByDefault
public final class LanguagePackManager {

    private static final Map<String, String> activeOverrides = new ConcurrentHashMap<>();

    // 內建 5 款特色官方語言包 (全文本深度覆蓋)
    public static final LanguagePack PACK_DEFAULT = new LanguagePack(
            "default",
            "預設官方語言 (Default)",
            "標準繁體/簡體中文官方詞條",
            true,
            null
    );

    private static volatile LanguagePack activePack = PACK_DEFAULT;

    public static final LanguagePack PACK_MEOW = new LanguagePack(
            "builtin:meow",
            "🐱 喵喵物語語言包 (Nya / Meow)",
            "全介面傲嬌可愛貓娘口癖，萌化方塊世界喵！",
            true,
            null
    );

    public static final LanguagePack PACK_CHUNIBYO = new LanguagePack(
            "builtin:chunibyo",
            "⚔️ 中二勇者物語語言包 (Hero / Anime)",
            "拔出聖劍，踏碎虛空！全介面熱血冒險勇者風格",
            true,
            null
    );

    public static final LanguagePack PACK_RETRO = new LanguagePack(
            "builtin:retro",
            "🕹️ 像素復古懷舊語言包 (Retro 8-Bit)",
            "回歸 80 年代街機卡帶像素浪漫風格",
            true,
            null
    );

    public static final LanguagePack PACK_CORPORATE = new LanguagePack(
            "builtin:corporate",
            "🏢 超級大廠黑話語言包 (Corporate Buzzword)",
            "深度賦能閉環、抓手底層打法、頂層戰略對齊",
            true,
            null
    );

    public static final LanguagePack PACK_MINIMAL = new LanguagePack(
            "builtin:minimal",
            "☕ 極簡特調咖啡語言包 (Minimalist Elegance)",
            "簡約優雅、去除雜質、專注方塊漫遊時光",
            true,
            null
    );

    static {
        ensureExamplePackExists();
    }

    private LanguagePackManager() {
    }

    /**
     * 取得自訂語言包存放目錄 (.zero/langpacks)
     */
    public static Path getLangPacksDirectory() {
        Path dir = Metadata.ZeroLauncher_LOCAL_HOME.resolve("langpacks");
        try {
            Files.createDirectories(dir);
        } catch (Throwable ignored) {
        }
        return dir;
    }

    /**
     * 確保語言包目錄下有範例設定檔供玩家參考
     */
    public static void ensureExamplePackExists() {
        try {
            Path dir = getLangPacksDirectory();
            Path example = dir.resolve("範例語言包_example.properties");
            if (!Files.exists(example)) {
                String content = """
                        # ==========================================
                        # Zero Launcher 自訂語言包範例
                        # 語法：Key=替換後的文字
                        # 特性：【寫什麼 替換什麼】！未填寫的詞條會自動保持預設官方翻譯。
                        # ==========================================

                        # 1. 首頁主按鈕文字
                        main.launch=🚀 衝啊！進入我的世界！

                        # 2. 設定頁面標題
                        settings=🔧 秘密控制台

                        # 3. 下載頁面標題
                        download=📥 模組與資源庫

                        # 4. 操作取消提示
                        message.cancelled=你剛剛取消了這次操作喔！
                        """;
                Files.writeString(example, content, StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 獲取所有可用語言包（內建 + 玩家自訂）
     */
    public static List<LanguagePack> getAvailablePacks() {
        List<LanguagePack> list = new ArrayList<>();
        list.add(PACK_DEFAULT);
        list.add(PACK_MEOW);
        list.add(PACK_CHUNIBYO);
        list.add(PACK_RETRO);
        list.add(PACK_CORPORATE);
        list.add(PACK_MINIMAL);

        // 掃描 .zero/langpacks/ 下所有 .properties, .lang, .json
        Path dir = getLangPacksDirectory();
        if (Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".properties") || fileName.endsWith(".lang") || fileName.endsWith(".json")) {
                        String id = "custom:" + fileName;
                        String displayName = "📁 " + fileName;
                        list.add(new LanguagePack(id, displayName, "自訂語言包：" + fileName, false, path));
                    }
                });
            } catch (Throwable e) {
                LOG.warning("Failed to list custom langpacks", e);
            }
        }

        return List.copyOf(list);
    }

    /**
     * 依 ID 尋找語言包
     */
    public static LanguagePack getPackById(String id) {
        for (LanguagePack pack : getAvailablePacks()) {
            if (pack.id().equalsIgnoreCase(id)) {
                return pack;
            }
        }
        return PACK_DEFAULT;
    }

    /**
     * 套用語言包
     */
    public static void applyPack(LanguagePack pack) {
        activePack = Objects.requireNonNullElse(pack, PACK_DEFAULT);
        activeOverrides.clear();

        if (activePack.isBuiltin()) {
            String resPath = switch (activePack.id()) {
                case "builtin:meow" -> "/assets/lang/packs/meow.properties";
                case "builtin:chunibyo" -> "/assets/lang/packs/chunibyo.properties";
                case "builtin:retro" -> "/assets/lang/packs/retro.properties";
                case "builtin:corporate" -> "/assets/lang/packs/corporate.properties";
                case "builtin:minimal" -> "/assets/lang/packs/minimal.properties";
                default -> null;
            };
            if (resPath != null) {
                loadBuiltinResource(resPath, activeOverrides);
            }
        } else if (activePack.customFile() != null && Files.exists(activePack.customFile())) {
            loadCustomFile(activePack.customFile(), activeOverrides);
        }

        LOG.info("Applied Language Pack: " + activePack.displayName() + " (Full Overrides count: " + activeOverrides.size() + ")");
    }

    /**
     * 載入內建完整語言包資源檔
     */
    private static void loadBuiltinResource(String resourceName, Map<String, String> targetMap) {
        try (InputStream is = LanguagePackManager.class.getResourceAsStream(resourceName)) {
            if (is != null) {
                Properties props = new Properties();
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                for (String key : props.stringPropertyNames()) {
                    targetMap.put(key, props.getProperty(key));
                }
            } else {
                LOG.warning("Builtin pack resource not found: " + resourceName);
            }
        } catch (Throwable t) {
            LOG.warning("Failed to load builtin pack resource: " + resourceName, t);
        }
    }

    /**
     * 讀取自訂語言包檔案 (.properties / .lang / .json)
     */
    private static void loadCustomFile(Path file, Map<String, String> targetMap) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".json")) {
                try (InputStream is = Files.newInputStream(file)) {
                    Map<String, String> jsonMap = JsonUtils.fromNonNullJsonFully(is, new TypeToken<Map<String, String>>() {});
                    targetMap.putAll(jsonMap);
                }
            } else {
                // .properties or .lang
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(file);
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                for (String key : props.stringPropertyNames()) {
                    targetMap.put(key, props.getProperty(key));
                }
            }
        } catch (Throwable t) {
            LOG.warning("Failed to load custom langpack: " + file, t);
        }
    }

    /**
     * 匯入自訂語言包檔案至 .zero/langpacks/
     */
    public static LanguagePack importCustomPack(Path sourceFile) throws IOException {
        Path targetDir = getLangPacksDirectory();
        String fileName = sourceFile.getFileName().toString();
        Path targetFile = targetDir.resolve(fileName);
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

        String id = "custom:" + fileName;
        LanguagePack pack = new LanguagePack(id, "📁 " + fileName, "自訂語言包：" + fileName, false, targetFile);
        applyPack(pack);
        return pack;
    }

    /**
     * 查詢詞條是否有被覆蓋
     */
    public static @Nullable String getOverride(String key) {
        return activeOverrides.get(key);
    }

    /**
     * 動態備用轉換器（確保任何未在字典的動態文字也能帶有風格）
     */
    public static String transformFallback(String key, String defaultText) {
        if (activePack == null || "default".equalsIgnoreCase(activePack.id()) || defaultText == null || defaultText.isEmpty()) {
            return defaultText;
        }
        if ("builtin:meow".equals(activePack.id())) {
            if (defaultText.endsWith("喵") || defaultText.endsWith("喵！") || defaultText.endsWith("喵～") || defaultText.endsWith("ฅ'ω'ฅ") || defaultText.endsWith("🐾")) {
                return defaultText;
            }
            if (defaultText.endsWith("。") || defaultText.endsWith("！") || defaultText.endsWith("!")) {
                return defaultText.substring(0, defaultText.length() - 1) + "喵！";
            }
            if (defaultText.endsWith("？") || defaultText.endsWith("?")) {
                return defaultText.substring(0, defaultText.length() - 1) + "喵？";
            }
            return defaultText + " 喵";
        }
        return defaultText;
    }

    /**
     * 取得當前套用的語言包
     */
    public static LanguagePack getActivePack() {
        return activePack;
    }
}
