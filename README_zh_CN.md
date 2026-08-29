<div align="center">

<img src="ZeroLauncher/src/main/resources/assets/img/icon@2x.png" alt="Zero Launcher Logo" width="128" height="128" />

# Zero Launcher

**现代、丝滑、极致美观的 Minecraft 启动器**  
*采用 Fluent 磨砂玻璃拟物风美学、内置原版 C418 音乐引擎与高性能架构打造。*

[![Release](https://img.shields.io/badge/Release-v1.0.0-38BDF8?style=for-the-badge&logo=github)](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases)
[![License: GPL v3.0](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blueviolet.svg?style=for-the-badge)](#-下载与运行)

[English](README.md) • [繁體中文](README_zh_TW.md) • [简体中文](README_zh_CN.md)

</div>

---

## 🌟 核心特色功能

### 🎨 1. 顶级磨砂玻璃质感 & 120 FPS 灵动动画
- **满版边框沉浸式壁纸**：无论切换深色或浅色模式，首页皆完美融合深色亚克力磨砂卡片，与自定义 Minecraft 壁纸相得益彰。
- **PCL2 级别灵动弹簧动画**：高响应贝塞尔曲线转场，页面切换利落丝滑、无任何卡顿掉帧。
- **Windows 11 圆润抗锯齿字体**：全域采用微软最新 *Segoe UI Variable* 与 *微软雅黑 UI*，并启用 LCD 次像素平滑渲染。

### 🎵 2. 内置 Minecraft 官方原版 C418 原声音乐引擎
- **官方高音质原声**：完整收录 C418 最经典曲目（*Sweden「灯..等等..灯」*、*Wet Hands*、*Subwoofer Lullaby*、*Haggstrom* 等）。
- **智慧动态音量调节**：根据系统音量自适应调节，舒适微柔、动听不吵闹。
- **游戏生命周期联动**：进入 Minecraft 游戏时自动暂停，关闭游戏回到启动器时自动无缝恢复播放。
- **顶部胶囊迷你播放器**：导航栏右上角 `[ 🎵 Sweden ]` 胶囊，左键随时暂停/播放，右键一秒切换下一首。

### 🧩 3. PCL2 风格模组批量管理
- **一键全选与实时选取计数**：支持全选、多选与动态标签计数。
- **强大批量操作**：一键批量启用、批量停用、联网 Modrinth/CurseForge 检查更新与安全批量移除。

### 📸 4. 游戏截图相册画廊 & 一键直贴社群
- **实时同步相册**：自动扫描游戏内按下 `F2` 的所有截图，依时间由新至旧呈现美观瀑布流。
- **超便利一键直贴**：点击 **`[ 📋 复制图片 ]`** 立即将图片写入系统剪贴板，在 Discord、微信、QQ、浏览器按 `Ctrl+V` 即可直接粘贴分享！
- **沉浸式大图灯箱**：全屏暗色灯箱预览，支持键盘方向键切换照片与快速定位文件。

### ⏳ 5. 世界存档时光机 & 一键备份还原
- **一键快照备份**：一键将世界存档压缩备份至 `.zero/backups/`。
- **历史存档时光倒流**：遭遇掉岩浆或坏档时，可一秒还原至任意历史存档点。
- **存档可视化卡片**：直接展示世界名称、游戏模式标签（*生存 / 创造 / 极限*）、最后游玩时间与存档大小。

### 📂 6. 全新纯净 `.zero` 架构与跨平台兼容
- **零残留纯净目录**：所有设置、缓存与实例元数据全面统一存放于 `.zero` 与 `zero.json`。
- **100% 无损向下兼容自动迁移**：初次启动自动无缝迁移旧版配置，账号与实例完全不受影响。
- **全加载器支持**：支持 Vanilla、Fabric、Forge、NeoForge、Quilt、Cleanroom、LiteLoader 与 OptiFine。
- **多账号体系**：支持微软官方 OAuth、离线账号与第三方 Authlib-Injector 外置登录。

---

## 📥 下载与运行

您可以在 [Releases](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases) 页面下载最新预编译发布包。

| 操作系统 | 推荐文件 | 系统需求 |
| :--- | :--- | :--- |
| **Windows** | `ZeroLauncher.jar` / `release/windows/ZeroLauncher.jar` | Java 17+ (推荐 Java 21) |
| **Linux** | `release/linux/ZeroLauncher.jar` + `ZeroLauncher.sh` | Java 17+ (OpenJFX / X11 或 Wayland) |

---

## 🛠️ 从源码编译

### 前置需求
- **JDK 17** 或 **JDK 21**（推荐 Eclipse Temurin 或 Liberica JDK Full）
- Git 工具

### 编译步骤

1. **克隆项目仓库：**
   ```bash
   git clone https://github.com/ZeroLauncherOfficial/ZeroLauncher.git
   cd ZeroLauncher
   ```

2. **使用 Gradle Wrapper 执行构建：**
   - **Windows:**
     ```cmd
     .\gradlew.bat jar -x test
     ```
   - **Linux / macOS:**
     ```bash
     chmod +x ./gradlew
     ./gradlew jar -x test
     ```

3. **获取编译产物：**
   编译产出的 JAR 文件位于 `HMCL/build/libs/HMCL-1.0.0.SNAPSHOT.jar`（或项目根目录之 `ZeroLauncher.jar`）。

---

## 🤝 参与贡献

欢迎提交 Pull Request、报告 Bug 或提出新功能建议！  
请至 [Issues](https://github.com/ZeroLauncherOfficial/ZeroLauncher/issues) 页面参与讨论。

---

## 📜 开源协议

Zero Launcher 基于 **[GNU 通用公共许可证第三版 (GPLv3.0)](LICENSE)** 开源。  
Minecraft 是 Mojang AB / Microsoft 的注册商标，Zero Launcher 与 Mojang 或 Microsoft 无任何关联。
