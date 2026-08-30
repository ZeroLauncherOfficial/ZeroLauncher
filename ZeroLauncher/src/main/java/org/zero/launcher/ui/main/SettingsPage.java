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
package org.zero.launcher.ui.main;

import com.jfoenix.controls.JFXButton;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.zero.launcher.Metadata;
import org.zero.launcher.task.Schedulers;
import org.zero.launcher.ui.Controllers;
import org.zero.launcher.ui.FXUtils;
import org.zero.launcher.ui.SVG;
import org.zero.launcher.ui.construct.*;
import org.zero.launcher.ui.construct.MessageDialogPane.MessageType;
import org.zero.launcher.upgrade.RemoteVersion;
import org.zero.launcher.upgrade.UpdateChannel;
import org.zero.launcher.upgrade.UpdateChecker;
import org.zero.launcher.upgrade.UpdateHandler;
import org.zero.launcher.util.Lang;
import org.zero.launcher.util.StringUtils;
import org.zero.launcher.util.i18n.I18n;
import org.zero.launcher.util.i18n.LanguagePack;
import org.zero.launcher.util.i18n.LanguagePackManager;
import org.zero.launcher.util.i18n.SupportedLocale;
import org.zero.launcher.util.io.FileUtils;
import org.zero.launcher.util.io.IOUtils;
import org.tukaani.xz.XZInputStream;
import javafx.stage.FileChooser;
import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.zero.launcher.setting.SettingsManager.settings;
import static org.zero.launcher.util.i18n.I18n.i18n;
import static org.zero.launcher.util.logging.Logger.LOG;

public final class SettingsPage extends ScrollPane {
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationListener updateListener;

    public SettingsPage() {
        this.setFitToWidth(true);

        VBox rootPane = new VBox(10);
        rootPane.setPadding(new Insets(10));
        this.setContent(rootPane);
        FXUtils.smoothScrolling(this);

        updateListener = any -> {};

        {
            ComponentList languagePaneList = new ComponentList();

            {
                var chooseLanguagePane = new LineSelectButton<SupportedLocale>();
                chooseLanguagePane.setTitle(i18n("settings.launcher.language"));
                chooseLanguagePane.setSubtitle(i18n("settings.take_effect_after_restart"));

                SupportedLocale currentLocale = I18n.getLocale();
                chooseLanguagePane.setNullSafeConverter(locale -> {
                    if (locale.isDefault())
                        return locale.getDisplayName(currentLocale);
                    else if (locale.isSameLanguage(currentLocale))
                        return locale.getDisplayName(locale);
                    else
                        return locale.getDisplayName(currentLocale) + " - " + locale.getDisplayName(locale);
                });
                chooseLanguagePane.setItems(SupportedLocale.getSupportedLocales());
                chooseLanguagePane.valueProperty().bindBidirectional(settings().languageProperty());

                languagePaneList.getContent().add(chooseLanguagePane);
            }

            {
                var choosePackPane = new LineSelectButton<LanguagePack>();
                choosePackPane.setTitle(i18n("settings.launcher.language_pack"));
                choosePackPane.setSubtitle(i18n("settings.launcher.language_pack.hint"));
                choosePackPane.setNullSafeConverter(LanguagePack::displayName);

                var packs = LanguagePackManager.getAvailablePacks();
                choosePackPane.setItems(packs);
                choosePackPane.setValue(LanguagePackManager.getPackById(settings().languagePackProperty().get()));

                choosePackPane.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        settings().languagePackProperty().set(newVal.id());
                        LanguagePackManager.applyPack(newVal);
                        Controllers.showToast(i18n("message.success"));
                    }
                });

                languagePaneList.getContent().add(choosePackPane);
            }

            {
                BorderPane packActionsPane = new BorderPane();

                Label leftLabel = new Label(i18n("settings.launcher.language_pack"));
                BorderPane.setAlignment(leftLabel, Pos.CENTER_LEFT);
                packActionsPane.setLeft(leftLabel);

                JFXButton importButton = FXUtils.newBorderButton(i18n("settings.launcher.language_pack.import"));
                importButton.setOnAction(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle(i18n("settings.launcher.language_pack.import"));
                    fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Language Pack (*.properties, *.lang, *.json)", "*.properties", "*.lang", "*.json")
                    );
                    File file = fileChooser.showOpenDialog(Controllers.getStage());
                    if (file != null) {
                        try {
                            LanguagePack imported = LanguagePackManager.importCustomPack(file.toPath());
                            settings().languagePackProperty().set(imported.id());
                            Controllers.showToast(i18n("settings.launcher.language_pack.import.success", file.getName()));
                        } catch (Exception ex) {
                            LOG.warning("Failed to import language pack", ex);
                            Controllers.dialog(i18n("settings.launcher.language_pack.import.failed", ex.getMessage()), null, MessageType.ERROR);
                        }
                    }
                });

                JFXButton openFolderButton = FXUtils.newBorderButton(i18n("settings.launcher.language_pack.open_folder"));
                openFolderButton.setOnAction(e -> FXUtils.openFolder(LanguagePackManager.getLangPacksDirectory()));

                HBox buttons = new HBox(10);
                buttons.setAlignment(Pos.CENTER_RIGHT);
                buttons.getChildren().addAll(importButton, openFolderButton);
                packActionsPane.setRight(buttons);

                languagePaneList.getContent().add(packActionsPane);
            }

            rootPane.getChildren().addAll(ComponentList.createComponentListTitle(i18n("settings.launcher.language")), languagePaneList);
        }

        {
            ComponentList miscPaneList = new ComponentList();

            {
                LineToggleButton disableAprilFools = new LineToggleButton();
                disableAprilFools.setTitle(i18n("settings.launcher.disable_april_fools"));
                disableAprilFools.setSubtitle(i18n("settings.take_effect_after_restart"));
                disableAprilFools.selectedProperty().bindBidirectional(settings().disableAprilFoolsProperty());
                miscPaneList.getContent().add(disableAprilFools);
            }

            {
                BorderPane debugPane = new BorderPane();

                Label left = new Label(i18n("settings.launcher.debug"));
                BorderPane.setAlignment(left, Pos.CENTER_LEFT);
                debugPane.setLeft(left);

                JFXButton openLogFolderButton = new JFXButton(i18n("settings.launcher.launcher_log.reveal"));
                openLogFolderButton.setOnAction(e -> openLogFolder());
                openLogFolderButton.getStyleClass().add("jfx-button-border");
                if (LOG.getLogFile() == null)
                    openLogFolderButton.setDisable(true);

                SpinnerPane exportLogPane = new SpinnerPane();

                JFXButton logButton = FXUtils.newBorderButton(i18n("settings.launcher.launcher_log.export"));
                exportLogPane.setContent(logButton);
                logButton.setOnAction(e -> {
                    exportLogPane.showSpinner();
                    onExportLogs().whenCompleteAsync((result, exception) -> {
                        exportLogPane.hideSpinner();
                        if (exception == null) {
                            Controllers.dialog(i18n("settings.launcher.launcher_log.export.success", result));
                            FXUtils.showFileInExplorer(result);
                        } else {
                            LOG.warning("Failed to export logs", exception);
                            Controllers.dialog(
                                    i18n("settings.launcher.launcher_log.export.failed") + "\n" + StringUtils.getStackTrace(exception),
                                    null,
                                    MessageType.ERROR
                            );
                        }
                    }, Schedulers.javafx());
                });

                HBox buttonBox = new HBox();
                buttonBox.setSpacing(10);
                buttonBox.getChildren().addAll(openLogFolderButton, exportLogPane);
                BorderPane.setAlignment(buttonBox, Pos.CENTER_RIGHT);
                debugPane.setRight(buttonBox);

                miscPaneList.getContent().add(debugPane);
            }

            rootPane.getChildren().addAll(ComponentList.createComponentListTitle(i18n("settings.launcher.misc")), miscPaneList);
        }
    }

    private void openLogFolder() {
        FXUtils.openFolder(LOG.getLogFile().getParent());
    }

    private void onUpdate() {
        RemoteVersion target = UpdateChecker.getLatestVersion();
        if (target == null) {
            return;
        }
        UpdateHandler.updateFrom(target);
    }

    private static String getEntryName(Set<String> entryNames, String name) {
        if (entryNames.add(name)) {
            return name;
        }

        for (long i = 1; ; i++) {
            String newName = name + "." + i;
            if (entryNames.add(newName)) {
                return newName;
            }
        }
    }

    /// This method guarantees to close both `input` and the current zip entry.
    ///
    /// If no exception occurs, this method returns `true`;
    /// If an exception occurs while reading from `input`, this method returns `false`;
    /// If an exception occurs while writing to `output`, this method will throw it as is.
    private static boolean exportLogFile(ZipOutputStream output,
                                         Path file, // For logging
                                         String entryName,
                                         InputStream input,
                                         byte[] buffer) throws IOException {
        //noinspection TryFinallyCanBeTryWithResources
        try {
            output.putNextEntry(new ZipEntry(entryName));
            int read;
            while (true) {
                try {
                    read = input.read(buffer);
                    if (read <= 0)
                        return true;
                } catch (Throwable ex) {
                    LOG.warning("Failed to decompress log file " + file, ex);
                    return false;
                }

                output.write(buffer, 0, read);
            }
        } finally {
            try {
                input.close();
            } catch (Throwable ex) {
                LOG.warning("Failed to close log file " + file, ex);
            }
            output.closeEntry();
        }
    }

    private CompletableFuture<Path> onExportLogs() {
        return CompletableFuture.supplyAsync(Lang.wrap(() -> {
            String nameBase = "zero-exported-logs-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));
            List<Path> recentLogFiles = LOG.findRecentLogFiles(5);

            Path outputFile;
            if (recentLogFiles.isEmpty()) {
                outputFile = Metadata.CURRENT_DIRECTORY.resolve(nameBase + ".log");

                LOG.info("Exporting latest logs to " + outputFile);
                try (OutputStream output = Files.newOutputStream(outputFile)) {
                    LOG.exportLogs(output);
                }
            } else {
                outputFile = Metadata.CURRENT_DIRECTORY.resolve(nameBase + ".zip");

                LOG.info("Exporting latest logs to " + outputFile);

                byte[] buffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
                try (var os = Files.newOutputStream(outputFile);
                     var zos = new ZipOutputStream(os)) {

                    Set<String> entryNames = new HashSet<>();

                    for (Path path : recentLogFiles) {
                        String fileName = FileUtils.getName(path);
                        String extension = StringUtils.substringAfterLast(fileName, '.');

                        if ("gz".equals(extension) || "xz".equals(extension)) {
                            // If an exception occurs while decompressing the input file, we should
                            // ensure the input file and the current zip entry are closed,
                            // then copy the compressed file content as-is into a new entry in the zip file.

                            InputStream input = null;
                            try {
                                input = Files.newInputStream(path);
                                input = "gz".equals(extension)
                                        ? new GZIPInputStream(input)
                                        : new XZInputStream(input);
                            } catch (Throwable ex) {
                                LOG.warning("Failed to open log file " + path, ex);
                                IOUtils.closeQuietly(input, ex);
                                input = null;
                            }

                            String entryName = getEntryName(entryNames, StringUtils.substringBeforeLast(fileName, "."));
                            if (input != null && exportLogFile(zos, path, entryName, input, buffer))
                                continue;
                        }

                        // Copy the log file content as-is into a new entry in the zip file.
                        // If an exception occurs while decompressing the input file, we should
                        // ensure the input file and the current zip entry are closed.

                        InputStream input;
                        try {
                            input = Files.newInputStream(path);
                        } catch (Throwable ex) {
                            LOG.warning("Failed to open log file " + path, ex);
                            continue;
                        }

                        exportLogFile(zos, path, getEntryName(entryNames, fileName), input, buffer);
                    }

                    zos.putNextEntry(new ZipEntry(getEntryName(entryNames, "zero-latest.log")));
                    LOG.exportLogs(zos);
                    zos.closeEntry();
                }
            }

            return outputFile;
        }), Schedulers.io());
    }
}
