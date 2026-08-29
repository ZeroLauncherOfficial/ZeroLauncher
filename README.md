<div align="center">

<img src="ZeroLauncher/src/main/resources/assets/img/icon@2x.png" alt="Zero Launcher Logo" width="128" height="128" />

# Zero Launcher

**A Modern, Ultra-Smooth, and Beautiful Minecraft Launcher**  
*Crafted with Fluent Glassmorphism, Authentic C418 Music, and High-Performance Architecture.*

[![Release](https://img.shields.io/badge/Release-v1.0.0-38BDF8?style=for-the-badge&logo=github)](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases)
[![License: GPL v3.0](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2021%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-blueviolet.svg?style=for-the-badge)](#download)

[English](README.md) • [繁體中文](README_zh_TW.md) • [简体中文](README_zh_CN.md)

</div>

---

## 🌟 Highlights & Features

### 🎨 1. Frosted Glassmorphism & Fluent 120 FPS UI
- **Edge-to-Edge Minecraft Wallpapers**: Seamless dark/light frosted acrylic glass overlays that perfectly blend with any custom background.
- **Fluid Spring Animations**: High-response cubic bezier transitions delivering a silky smooth, lag-free 120 FPS desktop experience.
- **Windows 11 Typography**: Built with crystal-clear LCD subpixel anti-aliasing and modern rounded fonts (*Segoe UI Variable*, *Microsoft JhengHei UI*).

### 🎵 2. Authentic Minecraft C418 Soundtrack Engine
- **Original Studio Recordings**: Built-in authentic C418 soundtracks (*Sweden, Wet Hands, Subwoofer Lullaby, Haggstrom, Living Mice, Mice on Venus*).
- **Intelligent Ambient Gain**: Automatically balances volume with system loudness for cozy, unobtrusive background vibes.
- **Game Lifecycle Sync**: Automatically pauses when entering Minecraft and seamlessly resumes upon game exit.
- **Interactive Mini Player**: Clickable capsule on the top navigation bar with quick pause/resume and right-click track switching.

### 🧩 3. PCL2-Style Batch Mod Management
- **One-Click Select All & Filter**: Select all or specific mods in seconds with a live selection counter badge.
- **Batch Actions**: Enable, disable, check updates across Modrinth/CurseForge, and batch delete with confirmation dialogs.

### 📸 4. Screenshot Gallery & Instant Clipboard Sharing
- **Real-Time Photo Sync**: Automatically indexes screenshots captured with `F2` in-game and presents them in an elegant responsive waterfall grid.
- **Instant Clipboard Paste**: One-click **`[ Copy Image ]`** puts full image data straight into your OS clipboard — paste directly (`Ctrl+V`) into Discord, LINE, Facebook, or web chats!
- **Immersive Lightbox**: Full-screen dark acrylic preview with keyboard arrow navigation and details.

### ⏳ 5. World Time Machine & Instant Backup Engine
- **Instant Snapshots**: One-click backup of your singleplayer worlds to `.zero/backups/`.
- **Time Machine Restore**: Revert to any historical backup point in one second — never fear lava falls or corrupted worlds again.
- **Visual World Cards**: Inspect world seed, gamemode badges (*Survival / Creative / Hardcore*), last played timestamp, and dimensions.

### 📂 6. Clean `.zero` Architecture & Multi-Platform
- **Zero Pollution**: All configurations, caches, and instance metadata are stored neatly under `.zero` and `zero.json`.
- **100% Loss-Free Migration**: Automatically migrates legacy configurations on first launch.
- **Multi-Loader Support**: Full support for Vanilla, Fabric, Forge, NeoForge, Quilt, Cleanroom, LiteLoader, and OptiFine.
- **Multi-Account System**: Microsoft Official OAuth, Offline Accounts, and 3rd-party Authlib-Injector.

---

## 📥 Download

Pre-built binaries for **Windows** and **Linux** are available in the [Releases](https://github.com/ZeroLauncherOfficial/ZeroLauncher/releases) section.

| Platform | Recommended File | Requirements |
| :--- | :--- | :--- |
| **Windows** | `ZeroLauncher.jar` / `release/windows/ZeroLauncher.jar` | Java 17+ (Java 21 Recommended) |
| **Linux** | `release/linux/ZeroLauncher.jar` + `ZeroLauncher.sh` | Java 17+ (OpenJFX / Wayland or X11) |

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 17** or **JDK 21** (e.g. Eclipse Temurin, Liberica JDK with JavaFX)
- Git

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ZeroLauncherOfficial/ZeroLauncher.git
   cd ZeroLauncher
   ```

2. **Build the executable JAR with Gradle Wrapper:**
   - **Windows:**
     ```cmd
     .\gradlew.bat jar -x test
     ```
   - **Linux / macOS:**
     ```bash
     chmod +x ./gradlew
     ./gradlew jar -x test
     ```

3. **Locate built binary:**
   The output JAR will be generated at `HMCL/build/libs/HMCL-1.0.0.SNAPSHOT.jar` (or `ZeroLauncher.jar`).

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!  
Feel free to check out our [Issues](https://github.com/ZeroLauncherOfficial/ZeroLauncher/issues) page.

---

## 📜 License

Zero Launcher is licensed under the **[GNU General Public License v3.0 (GPLv3.0)](LICENSE)**.  
Minecraft is a trademark of Mojang AB / Microsoft. Zero Launcher is not affiliated with Mojang or Microsoft.
