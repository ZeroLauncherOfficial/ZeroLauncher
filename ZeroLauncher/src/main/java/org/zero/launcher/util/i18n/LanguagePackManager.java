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
import org.zero.launcher.Metadata;
import org.zero.launcher.setting.SettingsManager;
import org.zero.launcher.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

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
 * 玩家可自訂單一或多個詞條，未覆蓋的詞條自動向下繼承預設語言檔。
 *
 * @author Zero
 */
@NotNullByDefault
public final class LanguagePackManager {

    private static final Map<String, String> activeOverrides = new ConcurrentHashMap<>();
    private static volatile LanguagePack activePack;

    // 內建 5 款特色官方語言包
    public static final LanguagePack PACK_DEFAULT = new LanguagePack(
            "default",
            "預設官方語言 (Default)",
            "標準繁體/簡體中文官方詞條",
            true,
            null
    );

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

    private static final Map<String, Map<String, String>> BUILTIN_OVERLAYS = new HashMap<>();

    static {
        // 1. 喵喵物語詞條庫
        Map<String, String> meow = new HashMap<>();
        meow.put("main.launch", "🐾 衝進方塊世界喵！");
        meow.put("main.launch.game", "🐾 衝進方塊世界喵！");
        meow.put("main.instance_selector", "ฅ'ω'ฅ 選擇冒險地圖喵");
        meow.put("main.account", "👑 主人帳號");
        meow.put("settings", "🌸 貓窩秘密設定喵");
        meow.put("settings.type.global.manage", "🏡 全域冒險管理喵");
        meow.put("settings.launcher.general", "🐾 日常抓抓偏好");
        meow.put("settings.launcher.language", "🐱 貓咪語言小調色盤");
        meow.put("settings.launcher.appearance", "🎀 貓咪打扮換裝秀");
        meow.put("settings.game.section.basic", "🐟 基礎冒險魚乾");
        meow.put("settings.game.section.game", "🎮 遊戲進階調皮設定");
        meow.put("settings.launcher", "ฅ'ω'ฅ 啟動器設定喵");
        meow.put("settings.advanced.gpu_preference", "⚡ 顯卡神力偏好喵");
        meow.put("settings.advanced.graphics", "🌈 亮晶晶畫面設定喵");
        meow.put("download", "🐟 叼魚魚下載中心喵");
        meow.put("about", "🐾 關於 Zero Launcher 喵");
        meow.put("message.cancelled", "操作已經取消了喵～");
        meow.put("message.success", "太棒啦！成功完成了喵！");
        meow.put("message.warning", "主人注意！好像有危險喵！");
        meow.put("launch.failed", "嗚嗚...方塊被抓爛閃退了喵...");
        meow.put("account.title", "👑 主人的身份通行證");
        meow.put("account.manage", "🐱 奴才與主人名冊");
        meow.put("java.management", "☕ 貓咪最愛爪哇 (Java) 管理");
        meow.put("launch.state.logging_in", "正在為主人認證通行證喵...");
        meow.put("launch.state.dependencies", "正在為主人叼齊零件包喵...");
        meow.put("launch.state.waiting_launching", "貓貓伸懶腰，準備起飛喵！");
        meow.put("launch.state.java", "正在煮香噴噴的 Java 咖啡喵...");
        meow.put("version.manage", "📦 版本實例小寶箱");
        meow.put("mod.manage", "✨ 魔法模組罐頭");
        BUILTIN_OVERLAYS.put("builtin:meow", meow);

        // 2. 中二勇者詞條庫
        Map<String, String> chu = new HashMap<>();
        chu.put("main.launch", "⚔️ 拔出聖劍，踏碎虛空！");
        chu.put("main.launch.game", "⚔️ 拔出聖劍，踏碎虛空！");
        chu.put("settings", "📜 神域禁忌契約");
        chu.put("settings.type.global.manage", "🏰 諸神領域統御");
        chu.put("settings.launcher.general", "📜 基礎法則印記");
        chu.put("settings.launcher.language", "🔮 異界神諭水晶");
        chu.put("settings.launcher.appearance", "👗 幻象神裝幻化");
        chu.put("settings.game.section.basic", "⚔️ 勇者基礎能力值");
        chu.put("settings.game.section.game", "⚡ 奧義技能配置");
        chu.put("settings.advanced.gpu_preference", "🌌 靈魂核心渲染矩陣");
        chu.put("settings.advanced.graphics", "👁️ 萬物真實之眼 (畫質)");
        chu.put("download", "⚡ 召喚深淵魔力卷軸");
        chu.put("about", "📖 創世英雄錄");
        chu.put("message.cancelled", "時空裂隙已強制重置！");
        chu.put("message.success", "任務達成！勇者榮耀加身！");
        chu.put("message.warning", "⚠️ 警報！前方偵測到高能魔力反應！");
        chu.put("launch.failed", "💥 封印反噬！神聖結界崩塌...");
        chu.put("account.title", "⚜️ 勇者名冊與靈魂烙印");
        chu.put("java.management", "🧙‍♂️ 元素使者 (Java) 聖殿");
        chu.put("launch.state.logging_in", "正在與神界簽署契約...");
        chu.put("launch.state.dependencies", "正在凝聚創世粒子...");
        chu.put("launch.state.waiting_launching", "次元之門正在開啟！");
        BUILTIN_OVERLAYS.put("builtin:chunibyo", chu);

        // 3. 像素復古懷舊詞條庫
        Map<String, String> retro = new HashMap<>();
        retro.put("main.launch", "▶ PRESS START TO PLAY");
        retro.put("main.launch.game", "▶ PRESS START TO PLAY");
        retro.put("settings", "⚙️ SYSTEM CONFIG");
        retro.put("settings.type.global.manage", "💾 MEMORY CARD SLOTS");
        retro.put("settings.launcher.general", "🔧 GENERAL OPTIONS");
        retro.put("settings.launcher.language", "🌐 LANGUAGE SELECT");
        retro.put("settings.launcher.appearance", "🎨 CRT / PALETTE MODE");
        retro.put("settings.game.section.basic", "🕹️ BASIC SETUP");
        retro.put("settings.game.section.game", "🕹️ ADVANCED GAMEPLAY");
        retro.put("settings.advanced.gpu_preference", "📟 8-BIT / 16-BIT CHIPSET");
        retro.put("settings.advanced.graphics", "📺 VIDEO / CRT SCANLINES");
        retro.put("download", "💾 INSERT CARTRIDGE (ROM)");
        retro.put("about", "👾 CREDITS & HIGH SCORES");
        retro.put("message.cancelled", "OPERATION ABORTED [ESC]");
        retro.put("message.success", "STAGE CLEAR!! BONUS +5000");
        retro.put("message.warning", "⚠️ WARNING: DANGER AHEAD");
        retro.put("launch.failed", "💀 GAME OVER (INSERT COIN)");
        retro.put("account.title", "👤 PLAYER 1 / PLAYER 2");
        retro.put("java.management", "🕹️ BIOS / RUNTIME SETUP");
        retro.put("launch.state.logging_in", "AUTHENTICATING CARTRIDGE...");
        retro.put("launch.state.dependencies", "LOADING ROM INTO RAM...");
        retro.put("launch.state.waiting_launching", "BOOTING 8-BIT ENGINE...");
        BUILTIN_OVERLAYS.put("builtin:retro", retro);

        // 4. 超級大廠黑話詞條庫
        Map<String, String> corp = new HashMap<>();
        corp.put("main.launch", "📈 賦能閉環並推進底層落地");
        corp.put("main.launch.game", "📈 賦能閉環並推進底層落地");
        corp.put("settings", "📊 戰略頂層架構對齊");
        corp.put("settings.type.global.manage", "🎯 核心主航道戰略管理");
        corp.put("settings.launcher.general", "📋 通用業務矩陣配置");
        corp.put("settings.launcher.language", "📑 跨區域本地化協同中台");
        corp.put("settings.launcher.appearance", "🎭 品牌視覺識別規範 (VI)");
        corp.put("settings.game.section.basic", "📊 基礎能力指標水位");
        corp.put("settings.game.section.game", "🚀 核心場景調優抓手");
        corp.put("settings.advanced.gpu_preference", "🔋 算力資源調度與異構加速");
        corp.put("settings.advanced.graphics", "📈 視覺體驗全景監控");
        corp.put("download", "📦 核心資產快速交付中台");
        corp.put("about", "🏢 企業核心競爭力年報");
        corp.put("message.cancelled", "該業務線已及時止損下線");
        corp.put("message.success", "業務目標圓滿達成並沉澱方法論");
        corp.put("message.warning", "⚠️ 識別到潛在合規風險點");
        corp.put("launch.failed", "🚨 觸發 P0 事故複盤會議");
        corp.put("account.title", "🔑 用戶權限與身份治理中台");
        corp.put("java.management", "⚙️ 底層運行時基礎設施集群");
        corp.put("launch.state.logging_in", "正在拉通對齊身份權限鏈...");
        corp.put("launch.state.dependencies", "正在複用中台依賴組件庫...");
        corp.put("launch.state.waiting_launching", "正在形成業務閉環並發佈...");
        BUILTIN_OVERLAYS.put("builtin:corporate", corp);

        // 5. 極簡特調咖啡詞條庫
        Map<String, String> minimal = new HashMap<>();
        minimal.put("main.launch", "☕ 享受方塊時光");
        minimal.put("main.launch.game", "☕ 享受方塊時光");
        minimal.put("settings", "✨ 空間偏好");
        minimal.put("settings.type.global.manage", "🕊️ 總覽");
        minimal.put("settings.launcher.general", "🛋️ 常規");
        minimal.put("settings.launcher.language", "🌿 語言");
        minimal.put("settings.launcher.appearance", "🎨 風格");
        minimal.put("settings.game.section.basic", "🌱 基本");
        minimal.put("settings.game.section.game", "🎮 遊戲");
        minimal.put("settings.advanced.gpu_preference", "🧊 繪圖晶片");
        minimal.put("settings.advanced.graphics", "🖼️ 畫面");
        minimal.put("download", "🌱 精選資源");
        minimal.put("about", "🕊️ 關於此處");
        minimal.put("message.cancelled", "已取消");
        minimal.put("message.success", "已完成");
        minimal.put("message.warning", "注意");
        minimal.put("launch.failed", "🍃 暫時迷失了方向");
        minimal.put("account.title", "👤 身份");
        minimal.put("java.management", "☕ 運行環境");
        minimal.put("launch.state.logging_in", "正在連接...");
        minimal.put("launch.state.dependencies", "整理資源中...");
        minimal.put("launch.state.waiting_launching", "靜候啟程...");
        BUILTIN_OVERLAYS.put("builtin:minimal", minimal);

        activePack = PACK_DEFAULT;
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
            Map<String, String> map = BUILTIN_OVERLAYS.get(activePack.id());
            if (map != null) {
                activeOverrides.putAll(map);
            }
        } else if (activePack.customFile() != null && Files.exists(activePack.customFile())) {
            loadCustomFile(activePack.customFile(), activeOverrides);
        }

        LOG.info("Applied Language Pack: " + activePack.displayName() + " (Overrides count: " + activeOverrides.size() + ")");
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
     * 取得當前套用的語言包
     */
    public static LanguagePack getActivePack() {
        return activePack;
    }
}
