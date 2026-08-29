/*
 * ZeroLauncher
 * Copyright (C) 2021  Zero <Zero@zerolauncher.net> and contributors
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
import com.jfoenix.controls.JFXPopup;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.zero.launcher.Metadata;
import org.zero.launcher.download.DefaultDependencyManager;
import org.zero.launcher.download.DownloadProvider;
import org.zero.launcher.download.LibraryAnalyzer;
import org.zero.launcher.download.VersionList;
import org.zero.launcher.game.ZeroLauncherGameRepository;
import org.zero.launcher.game.Version;
import java.util.concurrent.CompletableFuture;
import static org.zero.launcher.download.LibraryAnalyzer.LibraryType.MINECRAFT;
import org.zero.launcher.auth.Account;
import org.zero.launcher.setting.Accounts;
import org.zero.launcher.setting.DownloadProviders;
import org.zero.launcher.setting.GameDirectory;
import org.zero.launcher.setting.GameDirectoryManager;
import org.zero.launcher.task.Schedulers;
import org.zero.launcher.task.Task;
import org.zero.launcher.theme.Themes;
import org.zero.launcher.ui.Controllers;
import org.zero.launcher.ui.FXUtils;
import org.zero.launcher.ui.SVG;
import org.zero.launcher.ui.account.AccountListPopupMenu;
import org.zero.launcher.ui.animation.AnimationUtils;
import org.zero.launcher.ui.animation.ContainerAnimations;
import org.zero.launcher.ui.animation.TransitionPane;
import org.zero.launcher.ui.construct.MessageDialogPane;
import org.zero.launcher.ui.versions.GameListPopupMenu;
import org.zero.launcher.ui.construct.TwoLineListItem;
import org.zero.launcher.ui.decorator.DecoratorPage;
import org.zero.launcher.ui.versions.GameListPopupMenu;
import org.zero.launcher.ui.versions.Versions;
import org.zero.launcher.upgrade.RemoteVersion;
import org.zero.launcher.upgrade.UpdateChecker;
import org.zero.launcher.upgrade.UpdateHandler;
import org.zero.launcher.util.*;
import org.zero.launcher.util.i18n.I18n;
import org.zero.launcher.util.javafx.BindingMapping;
import org.zero.launcher.util.platform.OperatingSystem;
import org.zero.launcher.util.platform.Platform;
import org.zero.launcher.util.versioning.GameVersionNumber;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static org.zero.launcher.download.RemoteVersion.Type.RELEASE;
import static org.zero.launcher.setting.SettingsManager.state;
import static org.zero.launcher.ui.FXUtils.SINE;
import static org.zero.launcher.util.i18n.I18n.i18n;
import static org.zero.launcher.util.logging.Logger.LOG;

public final class MainPage extends StackPane implements DecoratorPage {
    private static final String ANNOUNCEMENT = "announcement";

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>();

    private final StringProperty currentGame = new SimpleStringProperty(this, "currentGame");
    private final BooleanProperty showUpdate = new SimpleBooleanProperty(this, "showUpdate");
    private final BooleanProperty showUpdateDialog = new SimpleBooleanProperty(this, "showUpdateDialog");
    private final ObjectProperty<RemoteVersion> latestVersion = new SimpleObjectProperty<>(this, "latestVersion");
    private final ObservableList<Version> versions = FXCollections.observableArrayList();
    private ZeroLauncherGameRepository repository;

    private TransitionPane announcementPane;
    private final StackPane updatePane;
    private final JFXButton menuButton = new JFXButton();

    private final Label lblInstanceHeroTitle = new Label();
    private final Label lblSelectorName = new Label();
    private final Label lblBadgeVersion = new Label("Minecraft");
    private final Label lblBadgeLoader = new Label("Instance");
    private final Label lblBadgeStatus = new Label("✓ Ready");
    private final Label lblRecentInstanceName = new Label();
    private final Label lblRecentInstanceSub = new Label();
    private final Label launchLabel = new Label();
    private final JFXButton launchButton = new JFXButton();

    private RemoteVersion lastShownVersion;

    {
        HBox titleNode = new HBox(8);
        titleNode.setPadding(new Insets(0, 0, 0, 2));
        titleNode.setAlignment(Pos.CENTER_LEFT);

        ImageView titleIcon = new ImageView(FXUtils.newBuiltinImage("/assets/img/icon-title.png"));
        Label titleLabel = new Label(Metadata.FULL_TITLE);
        if (I18n.isUpsideDown()) {
            titleIcon.setRotate(180);
            titleLabel.setRotate(180);
        }
        titleLabel.getStyleClass().add("jfx-decorator-title");
        titleLabel.textFillProperty().bind(Themes.titleFillProperty());
        titleNode.getChildren().setAll(titleIcon, titleLabel);

        state.setValue(new State(null, titleNode, false, false, true));

        updatePane = new StackPane();
        updatePane.setVisible(false);
        updatePane.getStyleClass().add("bubble");
        FXUtils.setLimitWidth(updatePane, 230);
        FXUtils.setLimitHeight(updatePane, 55);
        StackPane.setAlignment(updatePane, Pos.TOP_RIGHT);
        FXUtils.onClicked(updatePane, this::onUpgrade);
        updatePane.setCursor(Cursor.HAND);
        FXUtils.onChange(showUpdateProperty(), this::doAnimation);
        FXUtils.onChange(showUpdateDialogProperty(), this::showUpdateDialog);

        {
            HBox hBox = new HBox();
            hBox.setSpacing(12);
            hBox.setAlignment(Pos.CENTER_LEFT);
            StackPane.setAlignment(hBox, Pos.CENTER_LEFT);
            StackPane.setMargin(hBox, new Insets(9, 12, 9, 16));
            {
                TwoLineListItem prompt = new TwoLineListItem();
                prompt.setSubtitle(i18n("update.bubble.subtitle"));
                prompt.setPickOnBounds(false);
                prompt.titleProperty().bind(BindingMapping.of(latestVersionProperty()).map(latestVersion ->
                        latestVersion == null ? "" : i18n("update.bubble.title", latestVersion.version())));

                hBox.getChildren().setAll(SVG.UPDATE.createIcon(20), prompt);
            }

            JFXButton closeUpdateButton = new JFXButton();
            closeUpdateButton.setGraphic(SVG.CLOSE.createIcon(10));
            StackPane.setAlignment(closeUpdateButton, Pos.TOP_RIGHT);
            closeUpdateButton.getStyleClass().add("toggle-icon-tiny");
            StackPane.setMargin(closeUpdateButton, new Insets(5));
            closeUpdateButton.setOnAction(e -> closeUpdateBubble());

            updatePane.getChildren().setAll(hBox, closeUpdateButton);
        }

        // ==================== LEFT PANE: Welcome & Personal Area ====================
        VBox leftPane = new VBox(18);
        leftPane.setAlignment(Pos.CENTER_LEFT);
        leftPane.setMaxWidth(380);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        // 1. Greeting Typography Block
        VBox greetingBlock = new VBox(4);
        greetingBlock.setAlignment(Pos.CENTER_LEFT);

        Label lblGreetingPrefix = new Label("Good morning!");
        lblGreetingPrefix.getStyleClass().add("zero-greeting-prefix");

        Label lblPlayerName = new Label("Kairo");
        lblPlayerName.getStyleClass().add("zero-greeting-name");

        Label lblWelcomeMsg = new Label("歡迎回到 Zero Launcher。");
        lblWelcomeMsg.getStyleClass().add("zero-greeting-sub");

        FXUtils.onChangeAndOperate(Accounts.selectedAccountProperty(), (Account acc) -> {
            int hour = java.time.LocalTime.now().getHour();
            String prefix;
            if (hour >= 5 && hour < 12) {
                prefix = "Good morning!";
            } else if (hour >= 12 && hour < 18) {
                prefix = "Good afternoon!";
            } else {
                prefix = "Good evening!";
            }
            lblGreetingPrefix.setText(prefix);

            String username = acc != null && acc.getProfileName() != null && !acc.getProfileName().isBlank()
                    ? acc.getProfileName()
                    : "玩家";
            lblPlayerName.setText(username);
        });

        greetingBlock.getChildren().setAll(lblGreetingPrefix, lblPlayerName, lblWelcomeMsg);

        // 2. Recent Played Instance Card
        VBox recentCard = new VBox(8);
        recentCard.getStyleClass().add("zero-recent-play-card");
        recentCard.setCursor(Cursor.HAND);
        FXUtils.onClicked(recentCard, this::openInstanceSettings);

        Label lblRecentTitle = new Label("最近遊玩");
        lblRecentTitle.getStyleClass().add("zero-section-title");

        HBox recentContent = new HBox(12);
        recentContent.setAlignment(Pos.CENTER_LEFT);

        Node recentIcon = SVG.STADIA_CONTROLLER.createIcon(24);

        VBox recentInfo = new VBox(2);
        Label lblRecentInstanceName = new Label();
        lblRecentInstanceName.getStyleClass().add("zero-recent-instance-name");

        Label lblRecentInstanceSub = new Label();
        lblRecentInstanceSub.getStyleClass().add("zero-sub-hint");

        recentInfo.getChildren().setAll(lblRecentInstanceName, lblRecentInstanceSub);
        recentContent.getChildren().setAll(recentIcon, recentInfo);
        recentCard.getChildren().setAll(lblRecentTitle, recentContent);

        FXUtils.onChangeAndOperate(currentGameProperty(), (String currentGame) -> {
            if (currentGame != null) {
                lblRecentInstanceName.setText(currentGame);
                lblRecentInstanceSub.setText("已就緒 · 點擊查看設定");
            } else {
                lblRecentInstanceName.setText("尚未選擇遊戲實例");
                lblRecentInstanceSub.setText("點擊右側選擇實例開始遊玩");
            }
        });

        // 3. Player Account Card (Bottom of Left Pane)
        HBox playerAccountCard = new HBox(10);
        playerAccountCard.setAlignment(Pos.CENTER_LEFT);
        playerAccountCard.getStyleClass().add("zero-player-card");
        playerAccountCard.setCursor(Cursor.HAND);

        Node playerIcon = SVG.PERSON.createIcon(18);

        VBox playerDetail = new VBox(1);
        Label lblAccountUser = new Label("登入帳號");
        lblAccountUser.getStyleClass().add("zero-player-name");
        Label lblAccountType = new Label("點擊切換帳號");
        lblAccountType.getStyleClass().add("zero-player-type");
        playerDetail.getChildren().setAll(lblAccountUser, lblAccountType);

        Node switchIcon = SVG.UNFOLD_MORE.createIcon(16);
        HBox.setHgrow(playerDetail, Priority.ALWAYS);

        playerAccountCard.getChildren().setAll(playerIcon, playerDetail, switchIcon);
        FXUtils.onClicked(playerAccountCard, () -> Controllers.navigate(Controllers.getAccountListPage()));
        FXUtils.onSecondaryButtonClicked(playerAccountCard, () -> AccountListPopupMenu.show(playerAccountCard, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, playerAccountCard.getWidth(), 0));

        FXUtils.onChangeAndOperate(Accounts.selectedAccountProperty(), (Account acc) -> {
            if (acc != null && acc.getProfileName() != null && !acc.getProfileName().isBlank()) {
                lblAccountUser.setText(acc.getProfileName());
                String typeStr = acc instanceof org.zero.launcher.auth.microsoft.MicrosoftAccount ? "微軟正版"
                        : acc instanceof org.zero.launcher.auth.offline.OfflineAccount ? "離線帳號"
                        : "外置登入";
                lblAccountType.setText(typeStr + " · 切換");
            } else {
                lblAccountUser.setText("未登入帳號");
                lblAccountType.setText("點擊添加帳號");
            }
        });

        leftPane.getChildren().setAll(greetingBlock, recentCard, playerAccountCard);

        // ==================== RIGHT PANE: Minecraft Game Launch Area ====================
        VBox rightPane = new VBox(14);
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setMaxWidth(400);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // 1. Minecraft Hero Title & Version Badges
        VBox heroHeader = new VBox(6);
        heroHeader.setAlignment(Pos.CENTER);

        Label lblGameBrand = new Label("MINECRAFT");
        lblGameBrand.getStyleClass().add("zero-game-brand-label");

        lblInstanceHeroTitle.getStyleClass().add("zero-game-hero-title");

        HBox badgesRow = new HBox(8);
        badgesRow.setAlignment(Pos.CENTER);

        lblBadgeVersion.getStyleClass().add("zero-badge-green");
        lblBadgeLoader.getStyleClass().add("zero-badge-purple");
        lblBadgeStatus.getStyleClass().add("zero-badge-cyan");

        badgesRow.getChildren().setAll(lblBadgeVersion, lblBadgeLoader, lblBadgeStatus);
        heroHeader.getChildren().setAll(lblGameBrand, lblInstanceHeroTitle, badgesRow);

        // 2. Integrated PCL2-Style Dropdown Instance Selector
        BorderPane instanceSelector = new BorderPane();
        instanceSelector.getStyleClass().add("zero-instance-selector-bar");
        instanceSelector.setCursor(Cursor.HAND);
        FXUtils.onScroll(instanceSelector, versions, list -> {
            String currentId = getCurrentGame();
            return Lang.indexWhere(list, instance -> instance.getId().equals(currentId));
        }, it -> {
            ZeroLauncherGameRepository repo = repository != null ? repository : GameDirectoryManager.getSelectedRepository();
            if (repo != null) repo.setSelectedInstance(it.getId());
        });

        HBox instanceSelectorLeft = new HBox(8);
        instanceSelectorLeft.setAlignment(Pos.CENTER_LEFT);
        Node iconGame = SVG.STADIA_CONTROLLER.createIcon(18);
        lblSelectorName.getStyleClass().add("zero-instance-name");
        instanceSelectorLeft.getChildren().setAll(iconGame, lblSelectorName);

        HBox instanceSelectorRight = new HBox(4);
        instanceSelectorRight.setAlignment(Pos.CENTER_RIGHT);
        Label lblSwitchHint = new Label("切換實例");
        lblSwitchHint.getStyleClass().add("zero-sub-hint");
        Node dropDownIcon = SVG.ARROW_DROP_DOWN.createIcon(20);
        instanceSelectorRight.getChildren().setAll(lblSwitchHint, dropDownIcon);

        instanceSelector.setLeft(instanceSelectorLeft);
        instanceSelector.setRight(instanceSelectorRight);

        instanceSelector.setOnMouseClicked(e -> {
            ZeroLauncherGameRepository repo = repository != null ? repository : GameDirectoryManager.getSelectedRepository();
            if (repo == null) return;
            JFXPopup popup = GameListPopupMenu.showAndGetPopup(instanceSelector,
                    JFXPopup.PopupVPosition.BOTTOM,
                    JFXPopup.PopupHPosition.LEFT,
                    0,
                    -instanceSelector.getHeight() - 6,
                    repo,
                    versions);
            if (popup != null) {
                if (AnimationUtils.isAnimationEnabled()) {
                    Duration duration = Duration.millis(180);
                    RotateTransition rotateOpen = new RotateTransition(duration, dropDownIcon);
                    rotateOpen.setToAngle(-180);
                    FXUtils.playAnimation(dropDownIcon, "arrow-rotation", rotateOpen);

                    popup.setOnHidden(windowEvent -> {
                        RotateTransition rotateClose = new RotateTransition(duration, dropDownIcon);
                        rotateClose.setToAngle(0);
                        FXUtils.playAnimation(dropDownIcon, "arrow-rotation", rotateClose);
                    });
                } else {
                    dropDownIcon.setRotate(-180);
                    popup.setOnHidden(windowEvent -> dropDownIcon.setRotate(0));
                }
            }
        });

        // 3. Giant Hero Launch Button
        launchButton.getStyleClass().add("launch-button-hero");
        launchButton.setDefaultButton(true);

        launchLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        launchButton.setGraphic(launchLabel);

        FXUtils.onChangeAndOperate(currentGameProperty(), (String currentGame) -> updateInstanceUI());

        // 4. Quick Action Pills Row (Below Launch Button)
        HBox quickActionBar = new HBox(6);
        quickActionBar.setAlignment(Pos.CENTER);
        quickActionBar.getStyleClass().add("zero-quick-action-bar");

        JFXButton btnFolder = new JFXButton("目錄");
        btnFolder.setGraphic(SVG.FOLDER_OPEN.createIcon(14));
        btnFolder.getStyleClass().add("zero-quick-btn");
        btnFolder.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnFolder.setOnAction(e -> openCurrentDirectory());
        FXUtils.installFastTooltip(btnFolder, i18n("folder.game"));

        JFXButton btnMods = new JFXButton("模組");
        btnMods.setGraphic(SVG.EXTENSION.createIcon(14));
        btnMods.getStyleClass().add("zero-quick-btn");
        btnMods.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnMods.setOnAction(e -> openModsPage());
        FXUtils.installFastTooltip(btnMods, i18n("mods.manage"));

        JFXButton btnResources = new JFXButton("資源包");
        btnResources.setGraphic(SVG.TEXTURE.createIcon(14));
        btnResources.getStyleClass().add("zero-quick-btn");
        btnResources.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnResources.setOnAction(e -> openResourcePacksPage());
        FXUtils.installFastTooltip(btnResources, i18n("resourcepack.manage"));

        JFXButton btnScreenshots = new JFXButton("截圖");
        btnScreenshots.setGraphic(SVG.SCREENSHOT_MONITOR.createIcon(14));
        btnScreenshots.getStyleClass().add("zero-quick-btn");
        btnScreenshots.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnScreenshots.setOnAction(e -> openScreenshotsPage());
        FXUtils.installFastTooltip(btnScreenshots, "遊戲截圖相簿 (可一鍵複製貼到 Discord/LINE)");

        JFXButton btnWorlds = new JFXButton("存檔");
        btnWorlds.setGraphic(SVG.PUBLIC.createIcon(14));
        btnWorlds.getStyleClass().add("zero-quick-btn");
        btnWorlds.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnWorlds.setOnAction(e -> openWorldsPage());
        FXUtils.installFastTooltip(btnWorlds, "世界存檔時光機 (一鍵備份與還原)");

        JFXButton btnSettings = new JFXButton("設定");
        btnSettings.setGraphic(SVG.SETTINGS.createIcon(14));
        btnSettings.getStyleClass().add("zero-quick-btn");
        btnSettings.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnSettings.setOnAction(e -> openInstanceSettings());
        FXUtils.installFastTooltip(btnSettings, i18n("settings.game"));

        quickActionBar.getChildren().setAll(btnFolder, btnMods, btnResources, btnScreenshots, btnWorlds, btnSettings);

        rightPane.getChildren().setAll(heroHeader, instanceSelector, launchButton, quickActionBar);

        // ==================== FULL-BLEED BACKGROUND OVERLAY & MAIN SPLIT ====================
        javafx.scene.layout.Region darkOverlay = new javafx.scene.layout.Region();
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.40);");
        darkOverlay.setMouseTransparent(true);

        HBox mainSplit = new HBox(36);
        mainSplit.setAlignment(Pos.CENTER);
        mainSplit.setPadding(new Insets(24, 40, 24, 40));
        mainSplit.getChildren().setAll(leftPane, rightPane);
        StackPane.setAlignment(mainSplit, Pos.CENTER);

        getChildren().addAll(darkOverlay, updatePane, mainSplit);

    }

    private void showUpdateDialog(boolean show) {
        if (show && getLatestVersion() != null && !Objects.equals(getLatestVersion(), lastShownVersion)
                && !Objects.equals(state().getPromptedVersion(), getLatestVersion().version())
        ) {
            lastShownVersion = getLatestVersion();
            Controllers.dialogLater(new MessageDialogPane.Builder("", i18n("update.bubble.title", getLatestVersion().version()), MessageDialogPane.MessageType.INFO)
                    .addAction(i18n("button.view"), () -> {
                        state().setPromptedVersion(getLatestVersion().version());
                        onUpgrade();
                    })
                    .addCancel(null)
                    .build());
        }
    }

    private void doAnimation(boolean show) {
        if (AnimationUtils.isAnimationEnabled()) {
            Duration duration = Duration.millis(320);
            Timeline nowAnimation = new Timeline();
            nowAnimation.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(updatePane.translateXProperty(), show ? 260 : 0, SINE)),
                    new KeyFrame(duration,
                            new KeyValue(updatePane.translateXProperty(), show ? 0 : 260, SINE)));
            if (show) nowAnimation.getKeyFrames().add(
                    new KeyFrame(Duration.ZERO, e -> updatePane.setVisible(true)));
            else nowAnimation.getKeyFrames().add(
                    new KeyFrame(duration, e -> updatePane.setVisible(false)));
            nowAnimation.play();
        } else {
            updatePane.setVisible(show);
        }
    }

    private void openCurrentDirectory() {
        ZeroLauncherGameRepository repo = GameDirectoryManager.getSelectedRepository();
        String current = getCurrentGame();
        if (repo != null && current != null) {
            FXUtils.openFolder(repo.getRunDirectory(current));
        } else if (repo != null) {
            FXUtils.openFolder(repo.getBaseDirectory());
        }
    }

    private void openModsPage() {
        ZeroLauncherGameRepository repo = GameDirectoryManager.getSelectedRepository();
        String current = getCurrentGame();
        if (repo != null && current != null) {
            Controllers.getVersionPage().setVersion(current, repo);
            Controllers.getVersionPage().showMods();
            Controllers.navigate(Controllers.getVersionPage());
        } else {
            Controllers.navigate(Controllers.getGameListPage());
        }
    }

    private void openResourcePacksPage() {
        ZeroLauncherGameRepository repo = GameDirectoryManager.getSelectedRepository();
        String current = getCurrentGame();
        if (repo != null && current != null) {
            Controllers.getVersionPage().setVersion(current, repo);
            Controllers.getVersionPage().showResourcePacks();
            Controllers.navigate(Controllers.getVersionPage());
        } else {
            Controllers.navigate(Controllers.getGameListPage());
        }
    }

    private void openScreenshotsPage() {
        ZeroLauncherGameRepository repo = GameDirectoryManager.getSelectedRepository();
        String current = getCurrentGame();
        if (repo != null && current != null) {
            Controllers.getVersionPage().setVersion(current, repo);
            Controllers.getVersionPage().showScreenshots();
            Controllers.navigate(Controllers.getVersionPage());
        } else {
            Controllers.navigate(Controllers.getGameListPage());
        }
    }

    private void openWorldsPage() {
        ZeroLauncherGameRepository repo = GameDirectoryManager.getSelectedRepository();
        String current = getCurrentGame();
        if (repo != null && current != null) {
            Controllers.getVersionPage().setVersion(current, repo);
            Controllers.getVersionPage().showWorldList();
            Controllers.navigate(Controllers.getVersionPage());
        } else {
            Controllers.navigate(Controllers.getGameListPage());
        }
    }

    private void openInstanceSettings() {
        ZeroLauncherGameRepository repo = GameDirectoryManager.getSelectedRepository();
        String current = getCurrentGame();
        if (repo != null && current != null) {
            Versions.modifyGameSettings(repo, current);
        } else {
            Controllers.navigate(Controllers.getGameListPage());
        }
    }

    private void launch() {
        ZeroLauncherGameRepository repository = GameDirectoryManager.getSelectedRepository();
        Versions.launch(repository, repository.getSelectedInstance());
    }

    private void launchNoGame() {
        DownloadProvider downloadProvider = DownloadProviders.getDownloadProvider();
        VersionList<?> versionList = downloadProvider.getVersionListById("game");

        Holder<String> gameVersionHolder = new Holder<>();
        Task<?> task = versionList.refreshAsync("")
                .thenSupplyAsync(() -> versionList.getVersions("").stream()
                        .filter(it -> it.getVersionType() == RELEASE)
                        .filter(it -> NativePatcher.checkSupportedStatus(GameVersionNumber.asGameVersion(it.getGameVersion()), Platform.SYSTEM_PLATFORM, OperatingSystem.SYSTEM_VERSION) != NativePatcher.SupportStatus.UNSUPPORTED)
                        .sorted()
                        .findFirst()
                        .orElseThrow(() -> new IOException("No versions found")))
                .thenComposeAsync(version -> {
                    ZeroLauncherGameRepository repository = GameDirectoryManager.getSelectedRepository();
                    DefaultDependencyManager dependency = repository.getDependency();
                    String gameVersion = gameVersionHolder.value = version.getGameVersion();

                    return dependency.gameBuilder()
                            .name(gameVersion)
                            .gameVersion(gameVersion)
                            .buildAsync();
                })
                .whenComplete(any -> GameDirectoryManager.getSelectedRepository().refreshVersions())
                .whenComplete(Schedulers.javafx(), (result, exception) -> {
                    if (exception == null) {
                        GameDirectoryManager.getSelectedRepository().setSelectedInstance(gameVersionHolder.value);
                        launch();
                    } else if (exception instanceof CancellationException) {
                        // User cancelled, silently dismiss
                    } else {
                        LOG.warning("Failed to install game", exception);
                        Controllers.dialog(StringUtils.getStackTrace(exception),
                                i18n("install.failed"),
                                MessageDialogPane.MessageType.WARNING);
                    }
                });
        Controllers.taskDialog(task, i18n("version.launch.empty.installing"), TaskCancellationAction.NORMAL);
    }

    private void onUpgrade() {
        RemoteVersion target = UpdateChecker.getLatestVersion();
        if (target == null) {
            return;
        }
        UpdateHandler.updateFrom(target);
    }

    private void closeUpdateBubble() {
        showUpdate.unbind();
        showUpdate.set(false);
    }

    @Override
    public ReadOnlyObjectWrapper<State> stateProperty() {
        return state;
    }

    public GameDirectory getGameDirectory() {
        return repository.getGameDirectory();
    }

    public ZeroLauncherGameRepository getRepository() {
        return repository;
    }

    public String getCurrentGame() {
        return currentGame.get();
    }

    public StringProperty currentGameProperty() {
        return currentGame;
    }

    public void setCurrentGame(String currentGame) {
        this.currentGame.set(currentGame);
    }

    public ObservableList<Version> getVersions() {
        return versions;
    }

    public boolean isShowUpdate() {
        return showUpdate.get();
    }

    public BooleanProperty showUpdateProperty() {
        return showUpdate;
    }

    public void setShowUpdate(boolean showUpdate) {
        this.showUpdate.set(showUpdate);
    }

    public boolean isShowUpdateDialog() {
        return showUpdateDialog.get();
    }

    public BooleanProperty showUpdateDialogProperty() {
        return showUpdateDialog;
    }

    public void setShowUpdateDialog(boolean showUpdateDialog) {
        this.showUpdateDialog.set(showUpdateDialog);
    }

    public RemoteVersion getLatestVersion() {
        return latestVersion.get();
    }

    public ObjectProperty<RemoteVersion> latestVersionProperty() {
        return latestVersion;
    }

    public void setLatestVersion(RemoteVersion latestVersion) {
        this.latestVersion.set(latestVersion);
    }

    public void initVersions(ZeroLauncherGameRepository repository, List<Version> versions) {
        FXUtils.checkFxUserThread();
        this.repository = repository;
        this.versions.setAll(versions);
        updateInstanceUI();
    }

    public void updateInstanceUI() {
        String current = getCurrentGame();
        ZeroLauncherGameRepository repo = repository != null ? repository : GameDirectoryManager.getSelectedRepository();
        if (current == null || current.isBlank() || (repo != null && repo.getVersionCount() == 0)) {
            if (launchLabel != null) launchLabel.setText("📥  安 裝 遊 戲 實 例");
            if (lblInstanceHeroTitle != null) lblInstanceHeroTitle.setText("未選擇實例");
            if (lblSelectorName != null) lblSelectorName.setText("尚未安裝遊戲實例");
            if (lblBadgeVersion != null) lblBadgeVersion.setText("Minecraft");
            if (lblBadgeLoader != null) lblBadgeLoader.setText("No Instance");
            if (lblBadgeStatus != null) lblBadgeStatus.setText("需安裝");
            if (lblRecentInstanceName != null) lblRecentInstanceName.setText("尚未選擇遊戲實例");
            if (lblRecentInstanceSub != null) lblRecentInstanceSub.setText("點擊右側選擇實例開始遊玩");
            if (launchButton != null) FXUtils.setOnActionWithCooldown(launchButton, MainPage.this::launchNoGame);
        } else {
            if (launchLabel != null) launchLabel.setText("🚀  啟  動  遊  戲");
            if (lblInstanceHeroTitle != null) lblInstanceHeroTitle.setText(current);
            if (lblSelectorName != null) lblSelectorName.setText(current);
            if (lblRecentInstanceName != null) lblRecentInstanceName.setText(current);
            if (lblRecentInstanceSub != null) lblRecentInstanceSub.setText("已就緒 · 點擊查看設定");

            if (repo != null) {
                CompletableFuture.supplyAsync(() -> {
                    java.util.Optional<String> mcVerOpt = repo.getGameVersion(current);
                    String mcVer = mcVerOpt.orElse("");
                    String loaderName = "Vanilla";
                    try {
                        LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(repo.getResolvedPreservingPatchesVersion(current), mcVerOpt.orElse(null));
                        for (LibraryAnalyzer.LibraryMark mark : analyzer) {
                            String libId = mark.getLibraryId();
                            if (MINECRAFT.getPatchId().equals(libId)) continue;
                            String ver = mark.getLibraryVersion();
                            if (ver != null) ver = ver.replaceAll("(?i)" + libId, "").trim();
                            String displayName = I18n.hasKey("install.installer." + libId) ? i18n("install.installer." + libId) : libId;
                            loaderName = ver != null && !ver.isBlank() ? displayName + " " + ver : displayName;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                    return new String[]{mcVer, loaderName};
                }).thenAcceptAsync(info -> {
                    if (lblBadgeVersion != null) lblBadgeVersion.setText(info[0].isBlank() ? "Minecraft" : "Minecraft " + info[0]);
                    if (lblBadgeLoader != null) lblBadgeLoader.setText(info[1]);
                    if (lblBadgeStatus != null) lblBadgeStatus.setText("✓ Ready");
                }, Schedulers.javafx());
            } else {
                if (lblBadgeVersion != null) lblBadgeVersion.setText("Minecraft");
                if (lblBadgeLoader != null) lblBadgeLoader.setText("Instance");
                if (lblBadgeStatus != null) lblBadgeStatus.setText("✓ Ready");
            }

            if (launchButton != null) FXUtils.setOnActionWithCooldown(launchButton, MainPage.this::launch);
        }
    }
}
