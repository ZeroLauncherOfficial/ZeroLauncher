<div align="center">

<img src="ZeroLauncher/src/main/resources/assets/img/icon@2x.png" alt="Zero Launcher Logo" width="128" height="128" />

# Zero Launcher

**A Modern and Beautiful Minecraft Launcher**

[![Release](https://img.shields.io/badge/Release-v1.0.0-38BDF8?style=for-the-badge&logo=github)](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases)
[![License: GPL v3.0](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blueviolet.svg?style=for-the-badge)](#-download)

[English](README.md) • [繁體中文](README_zh_TW.md) • [简体中文](README_zh_CN.md)

</div>

---

### ⚠️ Note from the Author

> **The author of this project knows almost nothing about coding. Dear senior developers and programming masters, please don't roast me, pretty please! This entire project was made by AI (Artificial Intelligence)!!! 🙏🥺**

---

## 📖 About Zero Launcher

**Zero Launcher** is a custom open-source Minecraft launcher designed with modern aesthetics and enhanced user experience, built on the solid foundation of ZeroLauncher.

---

## 💖 Special Thanks & Acknowledgments

We would like to express our deepest gratitude and respect to:

- **[羊咕 (yanggu0413)](https://github.com/yanggu0413)** — Special thanks for testing assistance!
- **[貓貓 (cat6666-me)](https://github.com/cat6666-me)** — Special thanks for testing assistance!
- **The HMCL Development Team & Contributors** (huanghongxun, Glavo, and all past and present contributors) — Thank you so much for creating such an incredible, powerful, and robust open-source Minecraft launcher. Without the years of dedication and hard work from the HMCL team, this project would not exist.
- **The Minecraft Modding & Open-Source Community** — For providing continuous tools, libraries, and inspiration.

---

## 📥 Download

Pre-built binaries for **Windows** and **Linux** can be downloaded in the [Releases](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases) section.

| Platform | Recommended File | Requirements |
| :--- | :--- | :--- |
| **Windows** | `ZeroLauncher.jar` / `release/windows/ZeroLauncher.jar` | Java 17+ (Java 21 Recommended) |
| **Linux** | `release/linux/ZeroLauncher.jar` + `ZeroLauncher.sh` | Java 17+ (OpenJFX / Wayland or X11) |

---

## 🛠️ Building from Source

```bash
git clone https://github.com/ZeroLauncherOfficial/ZeroLauncher.git
cd ZeroLauncher

# Windows
.\gradlew.bat jar -x test

# Linux / macOS
chmod +x ./gradlew
./gradlew jar -x test
```

The output JAR will be generated at `ZeroLauncher/build/libs/` or `ZeroLauncher.jar`.

---

## 📜 License

Zero Launcher is licensed under the **[GNU General Public License v3.0 (GPLv3.0)](LICENSE)**.  
Minecraft is a trademark of Mojang AB / Microsoft. Zero Launcher is not affiliated with Mojang or Microsoft.
