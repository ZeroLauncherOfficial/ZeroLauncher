rootProject.name = "ZeroLauncher"
include(
    "ZeroLauncher",
    "ZeroLauncherCore",
    "ZeroLauncherBoot"
)

val minecraftLibraries = listOf("ZeroLauncherTransformerDiscoveryService", "ZeroLauncherMultiMCBootstrap")
include(minecraftLibraries)

for (library in minecraftLibraries) {
    project(":$library").projectDir = file("minecraft/libraries/$library")
}
