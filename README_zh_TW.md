<div align="center">

<img src="ZeroLauncher/src/main/resources/assets/img/icon@2x.png" alt="Zero Launcher Logo" width="128" height="128" />

# Zero Launcher

**現代、絲滑、極致美觀的 Minecraft 啟動器**  
*採用 Fluent 磨砂玻璃擬物風美學、內建原版 C418 音樂引擎與高性能架構打造。*

[![Release](https://img.shields.io/badge/Release-v1.0.0-38BDF8?style=for-the-badge&logo=github)](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases)
[![License: GPL v3.0](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blueviolet.svg?style=for-the-badge)](#-下載與執行)

[English](README.md) • [繁體中文](README_zh_TW.md) • [简体中文](README_zh_CN.md)

</div>

---

## 🌟 核心特色功能

### 🎨 1. 頂級磨砂玻璃質感 & 120 FPS 靈動動畫
- **滿版邊框沉浸式桌布**：無論切換深色或淺色模式，首頁皆完美融合深色亞克力磨砂卡片，與自訂 Minecraft 壁紙相得益彰。
- **PCL2 級別靈動彈簧動畫**：高響應貝茲曲線轉場，頁面切換俐落絲滑、無任何卡頓掉幀。
- **Windows 11 圓潤抗鋸齒字體**：全域採用微軟最新 *Segoe UI Variable* 與 *微軟正黑體 UI*，並啟用 LCD 次像素平滑渲染。

### 🎵 2. 內建 Minecraft 官方原版 C418 原聲音樂引擎
- **官方高音質原聲**：完整收錄 C418 最經典曲目（*Sweden「燈..燈燈..燈」*、*Wet Hands*、*Subwoofer Lullaby*、*Haggstrom* 等）。
- **智慧動態音量調節**：根據系統音量自適應調節，舒適微柔、動聽不吵雜。
- **遊戲生命週期聯動**：進入 Minecraft 遊戲時自動暫停，關閉遊戲回到啟動器時自動無縫恢復播放。
- **頂部膠囊迷你播放器**：導航列右上角 `[ 🎵 Sweden ]` 膠囊，左鍵隨時暫停/播放，右鍵一秒切換下一首。

### 🧩 3. PCL2 風格模組批次管理
- **一鍵全選與即時選取計數**：支援全選、多選與動態標籤計數。
- **強大批次操作**：一鍵批次啟用、批次停用、連線 Modrinth/CurseForge 檢查更新與安全批次移除。

### 📸 4. 遊戲截圖相簿畫廊 & 一鍵直貼社群
- **即時同步相簿**：自動掃描遊戲內按下 `F2` 的所有截圖，依時間由新至舊呈現美觀瀑布流。
- **超便利一鍵直貼**：點擊 **`[ 📋 複製圖片 ]`** 立即將圖片寫入系統剪貼簿，在 Discord、LINE、Facebook 按 `Ctrl+V` 即可直接貼上分享！
- **沉浸式大圖燈箱**：全螢幕暗色燈箱預覽，支援鍵盤方向鍵切換照片與快速定位檔案。

### ⏳ 5. 世界存檔時光機 & 一鍵備份還原
- **一鍵快照備份**：一鍵將世界存檔壓縮備份至 `.zero/backups/`。
- **歷史存檔時光倒流**：遭遇手滑掉岩漿或壞檔時，可一秒還原至任意歷史存檔點。
- **存檔視覺化卡片**：直接展示世界名稱、遊戲模式標籤（*生存 / 創造 / 極限*）、最後遊玩時間與存檔大小。

### 📂 6. 全新純淨 `.zero` 架構與跨平台相容
- **零殘留純淨目錄**：所有設定、快取與實例中繼資料全面統一存放於 `.zero` 與 `zero.json`。
- **100% 無損向下相容自動遷移**：初次啟動自動無縫遷移舊版配置，帳號與實例完全不受影響。
- **全載入器支援**：支援 Vanilla、Fabric、Forge、NeoForge、Quilt、Cleanroom、LiteLoader 與 OptiFine。
- **多帳號體系**：支援微軟官方 OAuth、離線帳號與第三方 Authlib-Injector 外置登入。

---

## 📥 下載與執行

您可以在 [Releases](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases) 頁面下載最新預編譯發布檔。

| 作業系統 | 推薦檔案 | 系統需求 |
| :--- | :--- | :--- |
| **Windows** | `ZeroLauncher.jar` / `release/windows/ZeroLauncher.jar` | Java 17+ (推薦 Java 21) |
| **Linux** | `release/linux/ZeroLauncher.jar` + `ZeroLauncher.sh` | Java 17+ (OpenJFX / X11 或 Wayland) |

---

## 🛠️ 從原始碼編譯

### 前置需求
- **JDK 17** 或 **JDK 21**（推薦 Eclipse Temurin 或 Liberica JDK Full）
- Git 工具

### 編譯步驟

1. **複製專案倉庫：**
   ```bash
   git clone https://github.com/ZeroLauncherOfficial/ZeroLauncher.git
   cd ZeroLauncher
   ```

2. **使用 Gradle Wrapper 執行建置：**
   - **Windows:**
     ```cmd
     .\gradlew.bat jar -x test
     ```
   - **Linux / macOS:**
     ```bash
     chmod +x ./gradlew
     ./gradlew jar -x test
     ```

3. **取得編譯產物：**
   編譯產出的 JAR 檔案位於 `HMCL/build/libs/HMCL-1.0.0.SNAPSHOT.jar`（或專案根目錄之 `ZeroLauncher.jar`）。

---

## 🤝 參與貢獻

歡迎提交 Pull Request、回報 Bug 或提出新功能建議！  
請至 [Issues](https://github.com/ZeroLauncherOfficial/ZeroLauncher/issues) 頁面參與討論。

---

## 📜 開源授權

Zero Launcher 基於 **[GNU 通用公共許可證第三版 (GPLv3.0)](LICENSE)** 開源。  
Minecraft 是 Mojang AB / Microsoft 的註冊商標，Zero Launcher 與 Mojang 或 Microsoft 無任何關聯。
