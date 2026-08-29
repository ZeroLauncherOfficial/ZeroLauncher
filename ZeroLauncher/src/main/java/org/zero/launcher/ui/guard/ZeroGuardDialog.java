/*
 * ZeroLauncher
 * Copyright (C) 2026 Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.launcher.ui.guard;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.zero.launcher.game.GameRepository;
import org.zero.launcher.guard.DependencyIssue;
import org.zero.launcher.guard.ZeroGuardReport;
import org.zero.launcher.guard.ZeroGuardResolver;
import org.zero.launcher.guard.ZeroGuardScanner;
import org.zero.launcher.ui.Controllers;
import org.zero.launcher.ui.FXUtils;
import org.zero.launcher.ui.construct.DialogCloseEvent;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@NotNullByDefault
public final class ZeroGuardDialog extends StackPane {

    private final GameRepository repository;
    private final String versionId;
    private final ZeroGuardReport report;
    private final Runnable onLaunchProceed;

    private final VBox issueContainer;
    private final List<JFXCheckBox> checkBoxes = new ArrayList<>();
    private final JFXButton installButton;
    private final JFXButton forceLaunchButton;
    private final JFXButton cancelButton;
    private final ProgressIndicator progressIndicator;
    private final Label statusFeedbackLabel;

    public ZeroGuardDialog(
            GameRepository repository,
            String versionId,
            ZeroGuardReport report,
            Runnable onLaunchProceed) {

        this.repository = repository;
        this.versionId = versionId;
        this.report = report;
        this.onLaunchProceed = onLaunchProceed;

        setPrefWidth(580);
        setMaxWidth(580);

        VBox rootBox = new VBox(14);
        rootBox.setPadding(new Insets(20));

        // Header
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label shieldIcon = new Label("🛡");
        shieldIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #38BDF8;");

        VBox titleTextBox = new VBox(2);
        Label titleLabel = new Label("Zero Guard 啟動前安全性檢查");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: -monet-on-surface;");

        String subtitleText = report.hasErrors()
                ? "發現 " + report.getErrorCount() + " 個可能阻礙遊戲啟動的缺失前置或衝突"
                : "發現 " + report.getWarningCount() + " 個建議安裝的選用前置模組";
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subtitleLabel.setStyle(report.hasErrors() ? "-fx-text-fill: #EF4444;" : "-fx-text-fill: #F59E0B;");

        titleTextBox.getChildren().addAll(titleLabel, subtitleLabel);
        titleBox.getChildren().addAll(shieldIcon, titleTextBox);

        // Issues list in ScrollPane
        issueContainer = new VBox(10);
        populateIssues();

        ScrollPane scrollPane = new ScrollPane(issueContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(260);
        scrollPane.setPrefHeight(Math.min(260, report.getIssues().size() * 85 + 20));
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        FXUtils.smoothScrolling(scrollPane);

        // Status & Progress feedback
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(18, 18);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        statusFeedbackLabel = new Label();
        statusFeedbackLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        statusFeedbackLabel.setStyle("-fx-text-fill: -monet-on-surface-variant;");

        statusBox.getChildren().addAll(progressIndicator, statusFeedbackLabel);

        // Action Buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        cancelButton = new JFXButton("取消啟動");
        cancelButton.getStyleClass().add("dialog-cancel");
        cancelButton.setOnAction(e -> close());

        forceLaunchButton = new JFXButton("⚡ 仍然強制啟動");
        forceLaunchButton.getStyleClass().add("dialog-cancel");
        forceLaunchButton.setStyle("-fx-text-fill: #F59E0B;");
        forceLaunchButton.setOnAction(e -> {
            close();
            onLaunchProceed.run();
        });

        installButton = new JFXButton("🛠 修復並安裝前置");
        installButton.getStyleClass().add("dialog-accept");
        installButton.setStyle("-fx-background-color: #0284C7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;");
        installButton.setOnAction(e -> handleInstallAndLaunch());

        boolean hasInstallable = report.getIssues().stream().anyMatch(DependencyIssue::isInstallable);
        installButton.setVisible(hasInstallable);
        installButton.setManaged(hasInstallable);

        actionBox.getChildren().addAll(cancelButton, forceLaunchButton);
        if (hasInstallable) {
            actionBox.getChildren().add(installButton);
        }

        rootBox.getChildren().addAll(titleBox, scrollPane, statusBox, actionBox);
        getChildren().add(rootBox);
    }

    private void close() {
        fireEvent(new DialogCloseEvent());
    }

    private void populateIssues() {
        issueContainer.getChildren().clear();
        checkBoxes.clear();

        for (DependencyIssue issue : report.getIssues()) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(10, 14, 10, 14));
            card.setStyle("-fx-background-color: -monet-surface-container; -fx-background-radius: 10px; -fx-border-color: -monet-outline-variant; -fx-border-radius: 10px; -fx-border-width: 1px;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(issue.getLevel() == DependencyIssue.Level.ERROR ? "❌" : "⚠");
            badge.setStyle("-fx-font-size: 13px;");

            Label modTitle = new Label(issue.getSourceModName() + " (v" + issue.getSourceModVersion() + ")");
            modTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            modTitle.setStyle("-fx-text-fill: -monet-on-surface;");

            header.getChildren().addAll(badge, modTitle);

            Label desc = new Label(issue.getDescription());
            desc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            desc.setStyle("-fx-text-fill: -monet-on-surface-variant;");
            desc.setWrapText(true);

            card.getChildren().addAll(header, desc);

            if (issue.isInstallable()) {
                JFXCheckBox cb = new JFXCheckBox("自動下載並安裝 " + issue.getTargetModName() + " (" + report.getGameVersion() + ")");
                cb.setSelected(issue.isSelectedForInstall());
                cb.selectedProperty().addListener((o, oldVal, newVal) -> issue.setSelectedForInstall(newVal));
                checkBoxes.add(cb);
                card.getChildren().add(cb);
            }

            issueContainer.getChildren().add(card);
        }
    }

    private void handleInstallAndLaunch() {
        List<DependencyIssue> toInstall = report.getIssues().stream()
                .filter(DependencyIssue::isInstallable)
                .filter(DependencyIssue::isSelectedForInstall)
                .toList();

        if (toInstall.isEmpty()) {
            statusFeedbackLabel.setText("未選取任何需安裝的前置模組。");
            return;
        }

        installButton.setDisable(true);
        forceLaunchButton.setDisable(true);
        cancelButton.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        statusFeedbackLabel.setText("正在自 Modrinth 下載相依模組 (0/" + toInstall.size() + ")...");

        CompletableFuture.runAsync(() -> {
            int successCount = 0;
            for (int i = 0; i < toInstall.size(); i++) {
                DependencyIssue issue = toInstall.get(i);
                final int currentIndex = i + 1;
                Platform.runLater(() -> statusFeedbackLabel.setText("正在下載 " + issue.getTargetModName() + " (" + currentIndex + "/" + toInstall.size() + ")..."));

                boolean ok = ZeroGuardResolver.resolveAndInstall(
                        repository,
                        versionId,
                        report.getGameVersion(),
                        report.getLoaderType(),
                        issue).join();

                if (ok) {
                    successCount++;
                }
            }

            final int finalSuccess = successCount;
            Platform.runLater(() -> {
                statusFeedbackLabel.setText("已完成安裝 " + finalSuccess + " 個模組，正在進行二次掃描確認...");
                ZeroGuardReport newReport = ZeroGuardScanner.scan(repository, versionId);
                if (newReport.isClean() || !newReport.hasErrors()) {
                    statusFeedbackLabel.setText("✓ 所有前置模組已就緒！正在啟動遊戲...");
                    close();
                    onLaunchProceed.run();
                } else {
                    installButton.setDisable(false);
                    forceLaunchButton.setDisable(false);
                    cancelButton.setDisable(false);
                    progressIndicator.setVisible(false);
                    progressIndicator.setManaged(false);
                    statusFeedbackLabel.setText("部分相依性仍需處理，請確認清單或選擇強制啟動。");
                }
            });
        });
    }

    public static void show(GameRepository repository, String versionId, ZeroGuardReport report, Runnable onLaunchProceed) {
        ZeroGuardDialog dialog = new ZeroGuardDialog(repository, versionId, report, onLaunchProceed);
        Controllers.dialog(dialog);
    }
}
