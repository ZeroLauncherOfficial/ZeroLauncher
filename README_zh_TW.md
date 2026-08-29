<div align="center">

<img src="ZeroLauncher/src/main/resources/assets/img/icon@2x.png" alt="Zero Launcher Logo" width="128" height="128" />

# Zero Launcher

**現代、極致美觀的 Minecraft 啟動器**

[![Release](https://img.shields.io/badge/Release-v1.0.0-38BDF8?style=for-the-badge&logo=github)](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases)
[![License: GPL v3.0](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blueviolet.svg?style=for-the-badge)](#-下載與執行)

[English](README.md) • [繁體中文](README_zh_TW.md) • [简体中文](README_zh_CN.md)

</div>

---

### ⚠️ 作者的心裡話（求生指南）

> **本專案作者對程式幾乎一竅不通，各位程式大佬請勿噴，求求你了，這個專案是 AI 做的！！！🙏🥺**

---

## 📖 關於 Zero Launcher

**Zero Launcher** 是一個致力於提供現代化視覺美學與流暢體驗的開源 Minecraft 啟動器，專案建立於優秀強大的 HMCL 基礎之上進行重構與客製化開發。

---

## 💖 特別鳴謝與致謝

在此向以下團隊與熱心夥伴致上最崇高的敬意與感謝：

- **[羊咕 (yanggu0413)](https://github.com/yanggu0413)** — 特別感謝協助測試！
- **[貓貓 (cat6666-me)](https://github.com/cat6666-me)** — 特別感謝協助測試！
- **HMCL 開發團隊與全體開源貢獻者**（huanghongxun、Glavo 以及歷來所有貢獻者）— 非常感謝你們打造了如此強大、穩定且卓越的開源 Minecraft 啟動器基石。沒有 HMCL 團隊多年的付出與心血，就絕對不會有本專案的誕生！
- **Minecraft 模組與開源社群** — 感謝為 Minecraft 生態系提供豐富工具、函式庫與靈感的每一個人。

---

## 📥 下載與執行

您可以在 [Releases](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases) 頁面下載最新預編譯發布檔。

| 作業系統 | 推薦檔案 | 系統需求 |
| :--- | :--- | :--- |
| **Windows** | `ZeroLauncher.jar` / `release/windows/ZeroLauncher.jar` | Java 17+ (推薦 Java 21) |
| **Linux** | `release/linux/ZeroLauncher.jar` + `ZeroLauncher.sh` | Java 17+ (OpenJFX / X11 或 Wayland) |

---

## 🛠️ 從原始碼編譯

```bash
git clone https://github.com/ZeroLauncherOfficial/ZeroLauncher.git
cd ZeroLauncher

# Windows
.\gradlew.bat jar -x test

# Linux / macOS
chmod +x ./gradlew
./gradlew jar -x test
```

編譯產出的 JAR 檔案位於 `ZeroLauncher/build/libs/` 或專案根目錄之 `ZeroLauncher.jar`。

---

## 📜 開源授權

Zero Launcher 基於 **[GNU 通用公共許可證第三版 (GPLv3.0)](LICENSE)** 開源。  
Minecraft 是 Mojang AB / Microsoft 的註冊商標，Zero Launcher 與 Mojang 或 Microsoft 無任何關聯。
