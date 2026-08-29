# Contributing Guide

<!-- #BEGIN LANGUAGE_SWITCHER -->
**English** | 中文 ([简体](Contributing_zh.md), [繁體](Contributing_zh_Hant.md))
<!-- #END LANGUAGE_SWITCHER -->

## Build ZeroLauncher

### Requirements

To build the ZeroLauncher launcher, you need to install JDK 17 (or higher). You can download it here: [Download Liberica JDK](https://bell-sw.com/pages/downloads/#jdk-25-lts).

After installing the JDK, make sure the `JAVA_HOME` environment variable points to the required JDK directory.
You can check the JDK version that `JAVA_HOME` points to like this:

<details>
<summary>Windows</summary>

PowerShell:

```
PS > & "$env:JAVA_HOME/bin/java.exe" -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

<details>
<summary>Linux/FreeBSD</summary>

```
> $JAVA_HOME/bin/java -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

<details>
<summary>macOS</summary>

```
> /usr/libexec/java_home --exec java -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

### Get ZeroLauncher Source Code

- You can get the latest source code via [Git](https://git-scm.com/downloads):
  ```shell
  git clone https://github.com/ZeroLauncher-dev/ZeroLauncher.git
  cd ZeroLauncher
  ```
- You can manually download a specific version of the source code from the [GitHub Release page](https://github.com/ZeroLauncher-dev/ZeroLauncher/releases).

### Build ZeroLauncher

To build ZeroLauncher, switch to the root directory of the ZeroLauncher project and run the following command:

```shell
./gradlew clean makeExecutables
```

The built ZeroLauncher program files are located in the `ZeroLauncher/build/libs` subdirectory under the project root.

## Debug Options

> [!WARNING]
> This document describes ZeroLauncher's internal features, which we do not guarantee to be stable and may be modified or removed at any time.
>
> Please use these features with caution, as improper use may cause ZeroLauncher to behave abnormally or even crash.

ZeroLauncher provides a series of debug options to control the behavior of the launcher.

These options can be specified via environment variables or JVM parameters. If both are present, JVM parameters will override the environment variable settings.

| Environment Variable        | JVM Parameter                                | Function                                                  | Default Value                                                                                               | Additional Notes          |
|-----------------------------|----------------------------------------------|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|---------------------------|
| `ZeroLauncher_JAVA_HOME`            |                                              | Specifies the Java used to launch ZeroLauncher                    |                                                                                                             | Only effective for exe/sh |
| `ZeroLauncher_JAVA_OPTS`            |                                              | Specifies the default JVM parameters when launching ZeroLauncher  |                                                                                                             | Only effective for exe/sh |
| `ZeroLauncher_FORCE_GPU`            |                                              | Specifies whether to force GPU-accelerated rendering      | `false`                                                                                                     |                           |
| `ZeroLauncher_ANIMATION_FRAME_RATE` |                                              | Specifies the animation frame rate of ZeroLauncher                | `60`                                                                                                        |                           |
| `ZeroLauncher_LANGUAGE`             |                                              | Specifies the default language of ZeroLauncher                    | Uses the system default language                                                                            |                           |
| `ZeroLauncher_UI_SCALE`             |                                              | Specifies the UI scaling for ZeroLauncher                         | Uses the system's current scaling                                                                           | Supports scale factor (1.5), percentage (150%), or DPI (144dpi).                          |
|                             | `-Dhmcl.dir=<path>`                          | Specifies the current data folder of ZeroLauncher                 | `./.zero`                                                                                                   |                           |
|                             | `-Dhmcl.home=<path>`                         | Specifies the user data folder of ZeroLauncher                    | Windows: `%APPDATA%\.zero`<br>Linux/BSD: `$XDG_DATA_HOME/hmcl`<br>macOS: `~Library/Application Support/hmcl` |                           |
|                             | `-Dhmcl.self_integrity_check.disable=true`   | Disables self-integrity checks during updates             |                                                                                                             |                           |
|                             | `-Dhmcl.bmclapi.override=<url>`              | Specifies the API Root for BMCLAPI                        | `https://bmclapi2.bangbang93.com`                                                                           |                           |
|                             | `-Dhmcl.discoapi.override=<url>`             | Specifies the API Root for foojay Disco API               | `https://api.foojay.io/disco/v3.0`                                                                          |                           | 
| `ZeroLauncher_FONT`                 | `-Dhmcl.font.override=<font family>`         | Specifies the default font for ZeroLauncher                       | Uses the system default font                                                                                |                           |
|                             | `-Dhmcl.update_source.override=<url>`        | Specifies the update source for ZeroLauncher                      | `https://hmcl.huangyuhui.net/api/update_link`                                                               |                           |
|                             | `-Dhmcl.authlibinjector.location=<path>`     | Specifies the location of the authlib-injector JAR file   | Uses the built-in authlib-injector                                                                          |                           |
|                             | `-Dhmcl.openjfx.repo=<maven repository url>` | Adds a custom Maven repository for downloading OpenJFX    |                                                                                                             |                           |
|                             | `-Dhmcl.native.encoding=<encoding>`          | Specifies the native encoding                             | Uses the system's native encoding                                                                           |                           |
|                             | `-Dhmcl.microsoft.auth.id=<App ID>`          | Specifies the Microsoft OAuth App ID                      | Uses the built-in Microsoft OAuth App ID                                                                    |                           |
|                             | `-Dhmcl.curseforge.apikey=<Api Key>`         | Specifies the CurseForge API key                          | Uses the built-in CurseForge API key                                                                        |                           |
|                             | `-Dhmcl.native.backend=<auto/jna/none>`      | Specifies the native backend used by ZeroLauncher                 | `auto`                                                                                                      |                           |
|                             | `-Dhmcl.hardware.fastfetch=<true/false>`     | Specifies whether to use fastfetch for hardware detection | `true`                                                                                                      |                           |
