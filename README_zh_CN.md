<div align="center">

<img src="ZeroLauncher/src/main/resources/assets/img/icon@2x.png" alt="Zero Launcher Logo" width="128" height="128" />

# Zero Launcher

**现代、极致美观的 Minecraft 启动器**

[![Release](https://img.shields.io/badge/Release-v1.0.0-38BDF8?style=for-the-badge&logo=github)](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases)
[![License: GPL v3.0](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blueviolet.svg?style=for-the-badge)](#-下载与运行)

[English](README.md) • [繁體中文](README_zh_TW.md) • [简体中文](README_zh_CN.md)

</div>

---

### ⚠️ 作者的心里话（求生指南）

> **本项目作者对编程几乎一窍不通，各位编程大佬请勿喷，求求你了，这个项目是 AI 做的！！！🙏🥺**

---

## 📖 关于 Zero Launcher

**Zero Launcher** 是一个致力于提供现代化视觉美学与流畅体验的开源 Minecraft 启动器，项目建立于优秀强大的 HMCL 基础之上进行重构与定制开发。

---

## 💖 特别鸣谢与致谢

在此向以下团队与开源社区致上最崇高的敬意与感谢：

- **HMCL 开发团队与全体开源贡献者**（huanghongxun、Glavo 以及历来所有贡献者）— 非常感谢你们打造了如此强大、稳定且卓越的开源 Minecraft 启动器基石。没有 HMCL 团队多年的付出与心血，就绝对不会有本项目的诞生！
- **Minecraft 模组与开源社区** — 感谢为 Minecraft 生态提供丰富工具、库与灵感的每一个人。

---

## 📥 下载与运行

您可以在 [Releases](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases) 页面下载最新预编译发布包。

| 操作系统 | 推荐文件 | 系统需求 |
| :--- | :--- | :--- |
| **Windows** | `ZeroLauncher.jar` / `release/windows/ZeroLauncher.jar` | Java 17+ (推荐 Java 21) |
| **Linux** | `release/linux/ZeroLauncher.jar` + `ZeroLauncher.sh` | Java 17+ (OpenJFX / X11 或 Wayland) |

---

## 🛠️ 从源码编译

```bash
git clone https://github.com/ZeroLauncherOfficial/ZeroLauncher.git
cd ZeroLauncher

# Windows
.\gradlew.bat jar -x test

# Linux / macOS
chmod +x ./gradlew
./gradlew jar -x test
```

编译产出的 JAR 文件位于 `ZeroLauncher/build/libs/` 或项目根目录之 `ZeroLauncher.jar`。

---

## 📜 开源协议

Zero Launcher 基于 **[GNU 通用公共许可证第三版 (GPLv3.0)](LICENSE)** 开源。  
Minecraft 是 Mojang AB / Microsoft 的注册商标，Zero Launcher 与 Mojang 或 Microsoft 无任何关联。
