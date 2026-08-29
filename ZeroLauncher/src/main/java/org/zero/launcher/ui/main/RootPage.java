package org.zero.launcher.ui.main;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.zero.launcher.Metadata;
import org.zero.launcher.event.EventBus;
import org.zero.launcher.event.RefreshedVersionsEvent;
import org.zero.launcher.game.ZeroLauncherGameRepository;
import org.zero.launcher.game.ModpackHelper;
import org.zero.launcher.game.Version;
import org.zero.launcher.setting.Accounts;
import org.zero.launcher.setting.GameDirectory;
import org.zero.launcher.setting.GameDirectoryManager;
import org.zero.launcher.task.Schedulers;
import org.zero.launcher.task.Task;
import org.zero.launcher.ui.Controllers;
import org.zero.launcher.ui.FXUtils;
import org.zero.launcher.ui.SVG;
import org.zero.launcher.ui.account.AccountListPopupMenu;
import org.zero.launcher.ui.animation.ContainerAnimations;
import org.zero.launcher.ui.animation.Motion;
import org.zero.launcher.ui.animation.TransitionPane;
import org.zero.launcher.ui.construct.MessageDialogPane;
import org.zero.launcher.ui.decorator.DecoratorPage;
import org.zero.launcher.ui.download.ModpackInstallWizardProvider;
import org.zero.launcher.ui.nbt.NBTEditorPage;
import org.zero.launcher.ui.nbt.NBTFileType;
import org.zero.launcher.ui.versions.Versions;
import org.zero.launcher.upgrade.UpdateChecker;
import org.zero.launcher.util.Lang;
import org.zero.launcher.util.StringUtils;
import org.zero.launcher.util.TaskCancellationAction;
import org.zero.launcher.util.io.CompressingUtils;
import org.zero.launcher.util.io.FileUtils;
import org.zero.launcher.util.versioning.VersionNumber;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.zero.launcher.ui.FXUtils.runInFX;
import static org.zero.launcher.util.i18n.I18n.i18n;
import static org.zero.launcher.util.logging.Logger.LOG;

public class RootPage extends StackPane implements DecoratorPage {

    public enum TabId {
        HOME, GAME, MOD, DOWNLOAD, RESOURCES, SETTINGS
    }

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>();
    private final TransitionPane contentContainer = new TransitionPane();
    private final Map<TabId, JFXButton> tabButtons = new EnumMap<>(TabId.class);

    private TabId currentTab = TabId.HOME;
    private MainPage mainPage = null;

    public RootPage() {
        EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class)
                .register(event -> onRefreshedVersions((ZeroLauncherGameRepository) event.getSource()));

        ZeroLauncherGameRepository repository = GameDirectoryManager.getSelectedRepository();
        if (repository.isLoaded())
            onRefreshedVersions(GameDirectoryManager.getSelectedRepository());

        // Setup top navigation bar
        HBox topNavBar = buildTopNavBar();
        state.setValue(new State(null, topNavBar, false, false, false));

        // Setup root content container
        contentContainer.setId("rootContentTransitionPane");
        getChildren().setAll(contentContainer);

        // Select initial tab
        selectTab(TabId.HOME);
    }

    private HBox buildTopNavBar() {
        HBox topNavBar = new HBox(12);
        topNavBar.setAlignment(Pos.CENTER_LEFT);
        topNavBar.setPadding(new Insets(0, 8, 0, 8));
        topNavBar.getStyleClass().add("zero-top-nav-bar");
        HBox.setHgrow(topNavBar, Priority.ALWAYS);

        // 1. Brand / Logo (Left)
        HBox brandBox = new HBox(6);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.getStyleClass().add("zero-nav-brand");
        brandBox.setCursor(Cursor.HAND);

        Label logoCloud = new Label("\u2601");
        logoCloud.setStyle("-fx-font-size: 17px; -fx-text-fill: #38BDF8;");

        Label logoTitle = new Label("Zero Launcher");
        logoTitle.getStyleClass().add("zero-brand-title");

        brandBox.getChildren().setAll(logoCloud, logoTitle);
        FXUtils.onClicked(brandBox, () -> selectTab(TabId.HOME));

        // 2. Navigation Tabs (Center)
        HBox navTabsBox = new HBox(4);
        navTabsBox.setAlignment(Pos.CENTER);
        navTabsBox.getStyleClass().add("zero-nav-tabs");
        HBox.setHgrow(navTabsBox, Priority.ALWAYS);

        addNavTab(navTabsBox, TabId.HOME, "首頁", SVG.HOME);
        addNavTab(navTabsBox, TabId.GAME, "遊戲", SVG.STADIA_CONTROLLER);
        addNavTab(navTabsBox, TabId.MOD, "Mod", SVG.EXTENSION);
        addNavTab(navTabsBox, TabId.DOWNLOAD, "下載", SVG.DOWNLOAD);
        addNavTab(navTabsBox, TabId.RESOURCES, "資源", SVG.TEXTURE);
        addNavTab(navTabsBox, TabId.SETTINGS, "設定", SVG.SETTINGS);

        topNavBar.getChildren().setAll(brandBox, navTabsBox);

        // 3. Mini BGM Widget (Right of top bar)
        HBox bgmWidget = new HBox(6);
        bgmWidget.setAlignment(Pos.CENTER_RIGHT);
        bgmWidget.getStyleClass().add("zero-nav-bgm-capsule");
        bgmWidget.setCursor(Cursor.HAND);

        Label lblBgmIcon = new Label("🎵");
        lblBgmIcon.setStyle("-fx-font-size: 13px; -fx-text-fill: #38BDF8;");
        Label lblBgmStatus = new Label("Sweden");
        lblBgmStatus.getStyleClass().add("zero-nav-bgm-label");

        bgmWidget.getChildren().setAll(lblBgmIcon, lblBgmStatus);

        org.zero.launcher.media.BGMManager bgm = org.zero.launcher.media.BGMManager.getInstance();
        FXUtils.onChangeAndOperate(bgm.currentTrackTitleProperty(), title -> {
            String shortName = title.contains("-") ? title.substring(title.indexOf('-') + 1).trim() : title;
            if (shortName.contains("(")) shortName = shortName.substring(0, shortName.indexOf('(')).trim();
            lblBgmStatus.setText(shortName);
        });
        FXUtils.onChangeAndOperate(bgm.playingProperty(), playing -> {
            if (playing) {
                lblBgmIcon.setText("🎵");
                lblBgmIcon.setOpacity(1.0);
            } else {
                lblBgmIcon.setText("⏸");
                lblBgmIcon.setOpacity(0.65);
            }
        });

        bgmWidget.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                bgm.toggle();
            } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                bgm.nextTrack();
            }
        });

        FXUtils.installFastTooltip(bgmWidget, "Minecraft 經典 BGM\n左鍵：播放 / 暫停\n右鍵：切換下一首");
        topNavBar.getChildren().add(bgmWidget);

        return topNavBar;
    }

    private void addNavTab(HBox container, TabId tabId, String label, SVG icon) {
        JFXButton btn = new JFXButton(label);
        btn.setGraphic(icon.createIcon(16));
        btn.getStyleClass().add("zero-nav-tab");
        btn.setOnAction(e -> selectTab(tabId));
        tabButtons.put(tabId, btn);
        container.getChildren().add(btn);
    }

    public void selectTab(TabId tabId) {
        this.currentTab = tabId;

        // Update active style on tabs
        tabButtons.forEach((id, btn) -> {
            if (id == tabId) {
                if (!btn.getStyleClass().contains("active")) {
                    btn.getStyleClass().add("active");
                }
            } else {
                btn.getStyleClass().remove("active");
            }
        });

        // Resolve page content
        Node pageNode = switch (tabId) {
            case HOME -> getMainPage();
            case GAME -> Controllers.getGameListPage();
            case MOD -> {
                String version = GameDirectoryManager.getSelectedRepository().getSelectedInstance();
                if (version != null) {
                    Controllers.getVersionPage().setVersion(version, GameDirectoryManager.getSelectedRepository());
                    Controllers.getVersionPage().showMods();
                    yield Controllers.getVersionPage();
                } else {
                    yield Controllers.getGameListPage();
                }
            }
            case DOWNLOAD -> {
                Controllers.getDownloadPage().showGameDownloads();
                yield Controllers.getDownloadPage();
            }
            case RESOURCES -> {
                String version = GameDirectoryManager.getSelectedRepository().getSelectedInstance();
                if (version != null) {
                    Controllers.getVersionPage().setVersion(version, GameDirectoryManager.getSelectedRepository());
                    Controllers.getVersionPage().showResourcePacks();
                    yield Controllers.getVersionPage();
                } else {
                    yield Controllers.getGameListPage();
                }
            }
            case SETTINGS -> {
                Controllers.getSettingsPage().showGameSettings(GameDirectoryManager.getSelectedRepository());
                yield Controllers.getSettingsPage();
            }
        };

        contentContainer.setContent(pageNode, ContainerAnimations.FADE, Motion.SHORT4);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public MainPage getMainPage() {
        if (mainPage == null) {
            MainPage mainPage = new MainPage();
            FXUtils.applyDragListener(mainPage,
                    file -> ModpackHelper.isFileModpackByExtension(file) || NBTFileType.isNBTFileByExtension(file) || "json".equalsIgnoreCase(FileUtils.getExtension(file)),
                    modpacks -> {
                        Path file = modpacks.get(0);
                        if (ModpackHelper.isFileModpackByExtension(file)) {
                            Controllers.getDecorator().startWizard(
                                    new ModpackInstallWizardProvider(GameDirectoryManager.getSelectedRepository(), file),
                                    i18n("install.modpack"));
                        } else if (NBTFileType.isNBTFileByExtension(file)) {
                            try {
                                Controllers.navigate(new NBTEditorPage(file));
                            } catch (Throwable e) {
                                LOG.warning("Fail to open nbt file", e);
                                Controllers.dialog(i18n("nbt.open.failed") + "\n\n" + StringUtils.getStackTrace(e),
                                        i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                            }
                        } else if ("json".equalsIgnoreCase(FileUtils.getExtension(file))) {
                            Versions.installFromJson(GameDirectoryManager.getSelectedRepository(), file);
                        }
                    });

            FXUtils.onChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(), mainPage::setCurrentGame);
            mainPage.latestVersionProperty().bind(UpdateChecker.latestVersionProperty());

            GameDirectoryManager.registerVersionsListener(repository -> {
                GameDirectory gameDirectory = repository.getGameDirectory();
                List<Version> children = repository.getVersions().parallelStream()
                        .filter(version -> !version.isHidden())
                        .sorted(Comparator
                                .comparing((Version version) -> Lang.requireNonNullElse(version.getReleaseTime(), Instant.EPOCH))
                                .thenComparing(version -> VersionNumber.asVersion(repository.getGameVersion(version).orElse(version.getId()))))
                        .collect(Collectors.toList());
                runInFX(() -> {
                    if (gameDirectory == GameDirectoryManager.getSelectedGameDirectory())
                        mainPage.initVersions(repository, children);
                });
            });
            this.mainPage = mainPage;
        }
        return mainPage;
    }

    private boolean checkedModpack = false;

    private void onRefreshedVersions(ZeroLauncherGameRepository repository) {
        runInFX(() -> {
            if (!checkedModpack) {
                checkedModpack = true;

                if (repository.getVersionCount() == 0) {
                    Path zipModpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.zip");
                    Path mrpackModpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.mrpack");

                    Path modpackFile;
                    if (Files.exists(zipModpack)) {
                        modpackFile = zipModpack;
                    } else if (Files.exists(mrpackModpack)) {
                        modpackFile = mrpackModpack;
                    } else {
                        modpackFile = null;
                    }

                    if (modpackFile != null) {
                        Task.supplyAsync(() -> CompressingUtils.findSuitableEncoding(modpackFile))
                                .thenApplyAsync(encoding -> ModpackHelper.readModpackManifest(modpackFile, encoding))
                                .thenApplyAsync(modpack -> ModpackHelper
                                        .getInstallTask(repository, modpackFile, modpack.getName(), modpack, null)
                                        .executor())
                                .thenAcceptAsync(Schedulers.javafx(), executor -> {
                                    Controllers.taskDialog(executor, i18n("modpack.installing"), TaskCancellationAction.NO_CANCEL);
                                    executor.start();
                                }).start();
                    }
                }
            }
        });
    }
}
