/*
 * ZeroLauncher
 * Copyright (C) 2020  Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.launcher.ui.export;

import javafx.scene.Node;
import org.zero.launcher.Metadata;
import org.zero.launcher.game.ZeroLauncherGameRepository;
import org.zero.launcher.modpack.ModAdviser;
import org.zero.launcher.modpack.ModpackExportInfo;
import org.zero.launcher.modpack.mcbbs.McbbsModpackExportTask;
import org.zero.launcher.modpack.modrinth.ModrinthModpackExportTask;
import org.zero.launcher.modpack.multimc.MultiMCInstanceConfiguration;
import org.zero.launcher.modpack.multimc.MultiMCModpackExportTask;
import org.zero.launcher.modpack.server.ServerModpackExportTask;
import org.zero.launcher.setting.*;
import org.zero.launcher.task.Task;
import org.zero.launcher.ui.wizard.WizardController;
import org.zero.launcher.ui.wizard.WizardProvider;
import org.zero.launcher.util.Lang;
import org.zero.launcher.util.SettingsMap;
import org.zero.launcher.util.gson.JsonUtils;
import org.zero.launcher.util.io.JarUtils;
import org.zero.launcher.util.io.Zipper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.zero.launcher.setting.SettingsManager.settings;

public final class ExportWizardProvider implements WizardProvider {
    private final ZeroLauncherGameRepository repository;
    private final String version;

    public ExportWizardProvider(ZeroLauncherGameRepository repository, String version) {
        this.repository = repository;
        this.version = version;
    }

    @Override
    public void start(SettingsMap settings) {
    }

    @Override
    public Object finish(SettingsMap settings) {
        List<String> whitelist = settings.get(ModpackFileSelectionPage.MODPACK_FILE_SELECTION);
        Path modpackFile = settings.get(ModpackInfoPage.MODPACK_FILE);
        ModpackExportInfo exportInfo = settings.get(ModpackInfoPage.MODPACK_INFO);
        exportInfo.setWhitelist(whitelist);
        String modpackType = settings.get(ModpackTypeSelectionPage.MODPACK_TYPE);

        return exportWithLauncher(modpackType, exportInfo, modpackFile);
    }

    private Task<?> exportWithLauncher(String modpackType, ModpackExportInfo exportInfo, Path modpackFile) {
        Path launcherJar = JarUtils.thisJarPath();
        boolean packWithLauncher = exportInfo.isPackWithLauncher() && launcherJar != null;
        return new Task<>() {
            Path tempModpack;
            Task<?> exportTask;

            {
                setSignificance(TaskSignificance.MODERATE);
            }

            @Override
            public boolean doPreExecute() {
                return true;
            }

            @Override
            public void preExecute() throws Exception {
                Path dest;
                if (packWithLauncher) {
                    dest = tempModpack = Files.createTempFile("zero", ".zip");
                } else {
                    dest = modpackFile;
                }

                switch (modpackType) {
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MCBBS:
                        exportTask = exportAsMcbbs(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MULTIMC:
                        exportTask = exportAsMultiMC(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_SERVER:
                        exportTask = exportAsServer(exportInfo, dest);
                        break;
                    case ModpackTypeSelectionPage.MODPACK_TYPE_MODRINTH:
                        exportTask = exportAsModrinth(exportInfo, dest);
                        break;
                    default:
                        throw new AssertionError("Unrecognized modpack type");
                }
            }

            @Override
            public Collection<Task<?>> getDependents() {
                return Collections.singleton(exportTask);
            }

            @Override
            public void execute() throws Exception {
                if (!packWithLauncher) return;
                try (Zipper zip = new Zipper(modpackFile)) {
                    LauncherSettings exported = new LauncherSettings();
                    LauncherSettings current = settings();

                    exported.versionListSourceProperty().set(current.versionListSourceProperty().get());
                    exported.fileDownloadSourceProperty().set(current.fileDownloadSourceProperty().get());
                    exported.preferredLoginTypeProperty().set(current.preferredLoginTypeProperty().get());

                    zip.putTextFile(exported.toJson(), ".zero/config/launcher-settings.json");
                    AuthlibInjectorServerList exportedServers = new AuthlibInjectorServerList();
                    exportedServers.getServers().setAll(SettingsManager.getAuthlibInjectorServers());
                    zip.putTextFile(
                            JsonUtils.GSON.toJson(exportedServers, AuthlibInjectorServerList.class),
                            ".zero/config/authlib-injector-servers.json");
                    zip.putFile(tempModpack, ModpackTypeSelectionPage.MODPACK_TYPE_MODRINTH.equals(modpackType)
                            ? "modpack.mrpack"
                            : "modpack.zip");

                    for (String extension : FontManager.FONT_EXTENSIONS) {
                        String fileName = "font." + extension;
                        Path font = Metadata.ZeroLauncher_LOCAL_HOME.resolve(fileName);
                        if (!Files.isRegularFile(font))
                            font = Metadata.CURRENT_DIRECTORY.resolve(fileName);
                        if (Files.isRegularFile(font))
                            zip.putFile(font, ".zero/" + fileName);
                    }

                    zip.putFile(launcherJar, launcherJar.getFileName().toString());
                }
            }
        };
    }

    private Task<?> exportAsMcbbs(ModpackExportInfo exportInfo, Path modpackFile) {
        return new Task<Void>() {
            Task<?> dependency = null;

            {
                setSignificance(TaskSignificance.MODERATE);
            }

            @Override
            public void execute() {
                dependency = new McbbsModpackExportTask(repository, version, exportInfo, modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsMultiMC(ModpackExportInfo exportInfo, Path modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            {
                setSignificance(TaskSignificance.MODERATE);
            }

            @Override
            public void execute() {
                GameSettings.Effective setting = repository.getEffectiveGameSettings(version);
                dependency = new MultiMCModpackExportTask(repository, version, exportInfo.getWhitelist(),
                        new MultiMCInstanceConfiguration(
                                "OneSix",
                                exportInfo.getName() + "-" + exportInfo.getVersion(),
                                null,
                                Lang.toIntOrNull(setting.getInheritable(GameSettings::permSizeProperty)),
                                setting.getInheritable(GameSettings::commandWrapperProperty),
                                setting.getInheritable(GameSettings::preLaunchCommandProperty),
                                null,
                                exportInfo.getDescription(),
                                null,
                                exportInfo.getJavaArguments(),
                                setting.getInheritable(GameSettings::windowTypeProperty) == GameWindowType.FULLSCREEN,
                                setting.getWidth(),
                                setting.getHeight(),
                                null,
                                exportInfo.getMinMemory(),
                                setting.getInheritable(GameSettings::showLogsProperty),
                                /* showConsoleOnError */ true,
                                /* autoCloseConsole */ false,
                                /* overrideMemory */ true,
                                /* overrideJavaLocation */ false,
                                /* overrideJavaArgs */ true,
                                /* overrideConsole */ true,
                                /* overrideCommands */ true,
                                /* overrideWindow */ true,
                                /* iconKey */ null // TODO
                        ), modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsServer(ModpackExportInfo exportInfo, Path modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            {
                setSignificance(TaskSignificance.MODERATE);
            }

            @Override
            public void execute() {
                dependency = new ServerModpackExportTask(repository, version, exportInfo, modpackFile);
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    private Task<?> exportAsModrinth(ModpackExportInfo exportInfo, Path modpackFile) {
        return new Task<Void>() {
            Task<?> dependency;

            {
                setSignificance(TaskSignificance.MODERATE);
            }

            @Override
            public void execute() {
                dependency = new ModrinthModpackExportTask(
                        repository,
                        version,
                        exportInfo,
                        modpackFile
                );
            }

            @Override
            public Collection<Task<?>> getDependencies() {
                return Collections.singleton(dependency);
            }
        };
    }

    @Override
    public Node createPage(WizardController controller, int step, SettingsMap settings) {
        return switch (step) {
            case 0 -> new ModpackTypeSelectionPage(controller);
            case 1 -> new ModpackInfoPage(controller, repository, version);
            case 2 -> new ModpackFileSelectionPage(controller, repository, version, ModAdviser::suggestMod);
            default -> throw new IllegalArgumentException("step");
        };
    }

    @Override
    public boolean cancel() {
        return true;
    }
}
