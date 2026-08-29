/*
 * ZeroLauncher
 * Copyright (C) 2026 Zero <Zero@zerolauncher.net> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.zero.hmcl.ui.versions;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.zero.hmcl.game.ZeroLauncherGameRepository;
import org.zero.hmcl.task.Task;
import org.zero.hmcl.ui.Controllers;
import org.zero.hmcl.ui.FXUtils;
import org.zero.hmcl.ui.SVG;
import org.zero.hmcl.ui.construct.DialogCloseEvent;
import org.zero.hmcl.ui.construct.PageAware;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.zero.hmcl.ui.ToolbarListPageSkin.createToolbarButton2;
import static org.zero.hmcl.util.i18n.I18n.i18n;

public final class ScreenshotGalleryPage extends StackPane implements VersionPage.GameInstanceLoadable, PageAware {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ZeroLauncherGameRepository repository;
    private String instanceId;
    private Path screenshotsDir;

    private final FlowPane gridPane = new FlowPane();
    private final Label lblCount = new Label();
    private final Label lblEmpty = new Label("目前尚無遊戲截圖，進入遊戲按下 F2 即可自動收錄美照！");
    private final List<ScreenshotItem> currentItems = new ArrayList<>();
    private final Map<Path, Image> thumbnailCache = new ConcurrentHashMap<>();

    private record ScreenshotItem(Path path, String name, long size, LocalDateTime time) {
    }

    public ScreenshotGalleryPage() {
        setPadding(new Insets(10));
        getStyleClass().addAll("notice-pane", "zero-screenshot-page");

        VBox root = new VBox(10);
        VBox.setVgrow(root, Priority.ALWAYS);

        // 1. Top Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 8, 4, 8));

        lblCount.setStyle("-fx-font-weight: bold; -fx-text-fill: -monet-primary; -fx-font-size: 13px; -fx-padding: 0 10 0 4;");

        toolbar.getChildren().setAll(
                createToolbarButton2(i18n("button.refresh"), SVG.REFRESH, this::refresh),
                createToolbarButton2("開啟截圖資料夾", SVG.FOLDER_OPEN, this::openScreenshotsFolder),
                lblCount
        );

        // 2. Center Grid / ScrollPane
        gridPane.setHgap(14);
        gridPane.setVgap(14);
        gridPane.setPadding(new Insets(10));
        gridPane.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        FXUtils.smoothScrolling(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        lblEmpty.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8; -fx-padding: 40px;");
        lblEmpty.setAlignment(Pos.CENTER);
        lblEmpty.setVisible(false);

        StackPane centerContainer = new StackPane(scrollPane, lblEmpty);
        VBox.setVgrow(centerContainer, Priority.ALWAYS);

        root.getChildren().setAll(toolbar, centerContainer);
        getChildren().setAll(root);
    }

    @Override
    public void loadInstance(ZeroLauncherGameRepository repository, String instanceId) {
        this.repository = repository;
        this.instanceId = instanceId;

        Path runDir = repository.getRunDirectory(instanceId);
        this.screenshotsDir = runDir.resolve("screenshots");
        refresh();
    }

    public void refresh() {
        if (screenshotsDir == null) return;

        Task.runAsync(() -> {
            List<ScreenshotItem> items = new ArrayList<>();
            try {
                if (Files.isDirectory(screenshotsDir)) {
                    try (var stream = Files.list(screenshotsDir)) {
                        stream.filter(p -> {
                            String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                        }).forEach(p -> {
                            try {
                                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                                LocalDateTime time = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                                items.add(new ScreenshotItem(p, p.getFileName().toString(), attr.size(), time));
                            } catch (Exception ignored) {
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }

            // Sort newest first
            items.sort((a, b) -> b.time().compareTo(a.time()));

            Platform.runLater(() -> {
                currentItems.clear();
                currentItems.addAll(items);
                renderItems();
            });
        });
    }

    private void renderItems() {
        gridPane.getChildren().clear();
        int total = currentItems.size();
        lblCount.setText("📸 遊戲截圖 (" + total + " 張)");

        if (total == 0) {
            lblEmpty.setVisible(true);
            return;
        }
        lblEmpty.setVisible(false);

        for (int i = 0; i < currentItems.size(); i++) {
            ScreenshotItem item = currentItems.get(i);
            int index = i;
            Node card = createScreenshotCard(item, index);
            gridPane.getChildren().add(card);
        }
    }

    private Node createScreenshotCard(ScreenshotItem item, int index) {
        VBox card = new VBox(6);
        card.getStyleClass().add("zero-screenshot-card");
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setPadding(new Insets(8));

        // 1. Thumbnail
        StackPane thumbContainer = new StackPane();
        thumbContainer.setPrefSize(204, 115);
        thumbContainer.setMinSize(204, 115);
        thumbContainer.setMaxSize(204, 115);
        thumbContainer.getStyleClass().add("zero-screenshot-thumb-box");
        thumbContainer.setCursor(Cursor.HAND);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(204);
        imageView.setFitHeight(115);
        imageView.setPreserveRatio(true);

        loadThumbnailAsync(item.path(), imageView);
        thumbContainer.getChildren().add(imageView);

        thumbContainer.setOnMouseClicked(e -> openLightbox(index));

        // 2. Info Row
        Label lblTime = new Label(item.time().format(TIME_FORMATTER));
        lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        // 3. Quick Action Buttons
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);

        JFXButton btnCopy = FXUtils.newToggleButton4(SVG.CONTENT_COPY);
        FXUtils.installFastTooltip(btnCopy, "複製圖片到剪貼簿 (可直接 Ctrl+V 貼上分享)");
        btnCopy.setOnAction(e -> copyImageToClipboard(item.path()));

        JFXButton btnOpen = FXUtils.newToggleButton4(SVG.FOLDER);
        FXUtils.installFastTooltip(btnOpen, "在檔案總管中顯示");
        btnOpen.setOnAction(e -> FXUtils.showFileInExplorer(item.path()));

        JFXButton btnDelete = FXUtils.newToggleButton4(SVG.DELETE_FOREVER);
        FXUtils.installFastTooltip(btnDelete, "刪除截圖");
        btnDelete.setOnAction(e -> {
            Controllers.confirm("確定要刪除這張截圖嗎？\n" + item.name(), "刪除截圖", () -> {
                try {
                    Files.deleteIfExists(item.path());
                    thumbnailCache.remove(item.path());
                    refresh();
                } catch (Exception ex) {
                    Controllers.showToast("刪除失敗：" + ex.getMessage());
                }
            }, null);
        });

        actions.getChildren().setAll(btnCopy, btnOpen, btnDelete);

        HBox bottomRow = new HBox(4, lblTime);
        HBox.setHgrow(lblTime, Priority.ALWAYS);
        bottomRow.getChildren().add(actions);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().setAll(thumbContainer, bottomRow);
        return card;
    }

    private void loadThumbnailAsync(Path path, ImageView imageView) {
        Image cached = thumbnailCache.get(path);
        if (cached != null) {
            imageView.setImage(cached);
            return;
        }

        Task.runAsync(() -> {
            try (InputStream in = Files.newInputStream(path)) {
                // Request 408px width thumbnail for smooth performance
                Image img = new Image(in, 408, 230, true, true);
                thumbnailCache.put(path, img);
                Platform.runLater(() -> imageView.setImage(img));
            } catch (Exception ignored) {
            }
        });
    }

    private void copyImageToClipboard(Path path) {
        try {
            Image img = new Image(Files.newInputStream(path));
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(img);
            List<File> files = Collections.singletonList(path.toFile());
            content.putFiles(files);
            clipboard.setContent(content);

            Controllers.showToast("📸 截圖已複製到剪貼簿！可直接在 Discord / LINE 按 Ctrl+V 貼上分享！");
        } catch (Exception e) {
            Controllers.showToast("複製失敗：" + e.getMessage());
        }
    }

    private void openScreenshotsFolder() {
        if (screenshotsDir != null) {
            try {
                Files.createDirectories(screenshotsDir);
                FXUtils.openFolder(screenshotsDir);
            } catch (Exception e) {
                Controllers.showToast("無法開啟資料夾：" + e.getMessage());
            }
        }
    }

    private void openLightbox(int initialIndex) {
        if (currentItems.isEmpty()) return;
        new LightboxDialog(initialIndex).show();
    }

    private final class LightboxDialog extends StackPane {
        private int currentIndex;
        private final ImageView bigImageView = new ImageView();
        private final Label lblTitle = new Label();
        private final Label lblIndex = new Label();

        LightboxDialog(int startIndex) {
            this.currentIndex = startIndex;
            setStyle("-fx-background-color: rgba(10, 15, 24, 0.94); -fx-background-radius: 12px;");
            setPadding(new Insets(16));

            Stage stage = Controllers.getStage();
            if (stage != null) {
                prefWidthProperty().bind(stage.widthProperty().multiply(0.88));
                prefHeightProperty().bind(stage.heightProperty().multiply(0.88));
            }

            VBox layout = new VBox(12);
            layout.setAlignment(Pos.CENTER);

            // Top bar
            HBox topBar = new HBox(12);
            topBar.setAlignment(Pos.CENTER_LEFT);

            lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
            lblIndex.setStyle("-fx-font-size: 12px; -fx-text-fill: #38BDF8;");
            HBox.setHgrow(lblTitle, Priority.ALWAYS);

            JFXButton btnCopyBig = new JFXButton("複製圖片");
            btnCopyBig.setGraphic(SVG.CONTENT_COPY.createIcon(16));
            btnCopyBig.getStyleClass().add("zero-quick-btn");
            btnCopyBig.setOnAction(e -> {
                if (currentIndex >= 0 && currentIndex < currentItems.size()) {
                    copyImageToClipboard(currentItems.get(currentIndex).path());
                }
            });

            JFXButton btnClose = createToolbarButton2(null, SVG.CLOSE, () -> fireEvent(new DialogCloseEvent()));

            topBar.getChildren().setAll(lblTitle, lblIndex, btnCopyBig, btnClose);

            // Center image with left/right buttons
            HBox imageRow = new HBox(12);
            imageRow.setAlignment(Pos.CENTER);
            VBox.setVgrow(imageRow, Priority.ALWAYS);

            JFXButton btnPrev = FXUtils.newToggleButton4(SVG.ARROW_BACK);
            btnPrev.setStyle("-fx-min-size: 44px; -fx-pref-size: 44px;");
            btnPrev.setOnAction(e -> showPrev());

            JFXButton btnNext = FXUtils.newToggleButton4(SVG.ARROW_FORWARD);
            btnNext.setStyle("-fx-min-size: 44px; -fx-pref-size: 44px;");
            btnNext.setOnAction(e -> showNext());

            bigImageView.setPreserveRatio(true);
            bigImageView.setSmooth(true);
            if (stage != null) {
                bigImageView.fitWidthProperty().bind(widthProperty().subtract(140));
                bigImageView.fitHeightProperty().bind(heightProperty().subtract(100));
            }

            imageRow.getChildren().setAll(btnPrev, bigImageView, btnNext);
            layout.getChildren().setAll(topBar, imageRow);
            getChildren().setAll(layout);

            updateImage();

            addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.LEFT) {
                    showPrev();
                    e.consume();
                } else if (e.getCode() == KeyCode.RIGHT) {
                    showNext();
                    e.consume();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    fireEvent(new DialogCloseEvent());
                    e.consume();
                }
            });
        }

        void show() {
            Controllers.dialog(this);
        }

        private void showPrev() {
            if (currentItems.isEmpty()) return;
            currentIndex = (currentIndex - 1 + currentItems.size()) % currentItems.size();
            updateImage();
        }

        private void showNext() {
            if (currentItems.isEmpty()) return;
            currentIndex = (currentIndex + 1) % currentItems.size();
            updateImage();
        }

        private void updateImage() {
            if (currentIndex < 0 || currentIndex >= currentItems.size()) return;
            ScreenshotItem item = currentItems.get(currentIndex);
            lblTitle.setText(item.name());
            lblIndex.setText((currentIndex + 1) + " / " + currentItems.size());

            Task.runAsync(() -> {
                try (InputStream in = Files.newInputStream(item.path())) {
                    Image fullImg = new Image(in);
                    Platform.runLater(() -> bigImageView.setImage(fullImg));
                } catch (Exception ignored) {
                }
            });
        }
    }
}
