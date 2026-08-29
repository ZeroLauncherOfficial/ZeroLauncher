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
package org.zero.hmcl.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.zero.hmcl.auth.Account;
import org.zero.hmcl.setting.Accounts;

import java.time.LocalTime;

public final class ZeroSplashScreen {

    private final Stage stage;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final StackPane rootCard;

    public ZeroSplashScreen() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle("ZeroLauncher");

        rootCard = new StackPane();
        rootCard.setPrefSize(420, 240);
        rootCard.setMaxSize(420, 240);
        rootCard.setStyle("-fx-background-color: rgba(18, 24, 32, 0.95); " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.12); " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: 1px;");
        rootCard.setEffect(new DropShadow(24, 0, 8, Color.rgb(0, 0, 0, 0.45)));

        VBox contentBox = new VBox(12);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(28, 32, 28, 32));

        // Brand Item
        HBox brandBox = new HBox(8);
        brandBox.setAlignment(Pos.CENTER);

        Label cloudIcon = new Label("\u2601");
        cloudIcon.setStyle("-fx-font-size: 26px; -fx-text-fill: #38BDF8;");

        Label brandTitle = new Label("ZERO LAUNCHER");
        brandTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brandTitle.setStyle("-fx-text-fill: #FFFFFF;");

        brandBox.getChildren().setAll(cloudIcon, brandTitle);

        // Personalized Greeting
        String greeting = getPersonalizedGreeting();
        Label greetingLabel = new Label(greeting);
        greetingLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 14));
        greetingLabel.setStyle("-fx-text-fill: #94A3B8;");

        // Progress indicator
        progressBar = new ProgressBar(-1);
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(4);

        // Status Label
        statusLabel = new Label("Loading...");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        statusLabel.setStyle("-fx-text-fill: #6474B8;");

        contentBox.getChildren().setAll(brandBox, greetingLabel, progressBar, statusLabel);
        rootCard.getChildren().add(contentBox);

        StackPane sceneRoot = new StackPane(rootCard);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(20));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
    }

    public void show() {
        stage.centerOnScreen();
        stage.show();

        rootCard.setOpacity(0);
        rootCard.setScaleX(0.96);
        rootCard.setScaleY(0.96);

        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(rootCard.opacityProperty(), 0),
                        new KeyValue(rootCard.scaleXProperty(), 0.96),
                        new KeyValue(rootCard.scaleYProperty(), 0.96)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(rootCard.opacityProperty(), 1.0),
                        new KeyValue(rootCard.scaleXProperty(), 1.0),
                        new KeyValue(rootCard.scaleYProperty(), 1.0))
        );
        fadeIn.play();
    }

    public void setStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    public void finish(Runnable onFinished) {
        Platform.runLater(() -> {
            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(rootCard.opacityProperty(), 1.0),
                            new KeyValue(rootCard.scaleXProperty(), 1.0),
                            new KeyValue(rootCard.scaleYProperty(), 1.0)),
                    new KeyFrame(Duration.millis(220),
                            new KeyValue(rootCard.opacityProperty(), 0),
                            new KeyValue(rootCard.scaleXProperty(), 0.96),
                            new KeyValue(rootCard.scaleYProperty(), 0.96))
            );
            fadeOut.setOnFinished(e -> {
                stage.close();
                if (onFinished != null) {
                    onFinished.run();
                }
            });
            fadeOut.play();
        });
    }

    private static String getPersonalizedGreeting() {
        int hour = LocalTime.now().getHour();
        String timeGreeting;
        if (hour >= 5 && hour < 12) {
            timeGreeting = "Good morning";
        } else if (hour >= 12 && hour < 18) {
            timeGreeting = "Good afternoon";
        } else {
            timeGreeting = "Good evening";
        }

        String username = "Player";
        try {
            Account account = Accounts.getSelectedAccount();
            if (account != null && account.getProfileName() != null && !account.getProfileName().isBlank()) {
                username = account.getProfileName();
            }
        } catch (Throwable ignored) {
        }

        return timeGreeting + ", " + username + ".";
    }
}