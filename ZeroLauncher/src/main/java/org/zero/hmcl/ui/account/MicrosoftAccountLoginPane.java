/*
 * ZeroLauncher
 * Copyright (C) 2026  Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.hmcl.ui.account;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXSpinner;
import io.nayuki.qrcodegen.QrCode;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import org.zero.hmcl.auth.Account;
import org.zero.hmcl.auth.AuthInfo;
import org.zero.hmcl.auth.AuthenticationException;
import org.zero.hmcl.auth.OAuth;
import org.zero.hmcl.auth.microsoft.MicrosoftAccount;
import org.zero.hmcl.auth.yggdrasil.YggdrasilService;
import org.zero.hmcl.setting.Accounts;
import org.zero.hmcl.task.Schedulers;
import org.zero.hmcl.task.Task;
import org.zero.hmcl.task.TaskExecutor;
import org.zero.hmcl.theme.Themes;
import org.zero.hmcl.ui.Controllers;
import org.zero.hmcl.ui.FXUtils;
import org.zero.hmcl.ui.WeakListenerHolder;
import org.zero.hmcl.ui.construct.*;
import org.zero.hmcl.upgrade.IntegrityChecker;
import org.zero.hmcl.util.Lang;
import org.zero.hmcl.util.QrCodeUtils;
import org.zero.hmcl.util.StringUtils;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static org.zero.hmcl.setting.SettingsManager.settings;
import static org.zero.hmcl.ui.FXUtils.onEscPressed;
import static org.zero.hmcl.util.i18n.I18n.i18n;

public class MicrosoftAccountLoginPane extends JFXDialogLayout implements DialogAware {
    private final Account accountToRelogin;
    private final Consumer<AuthInfo> loginCallback;
    private final Runnable cancelCallback;

    @SuppressWarnings("FieldCanBeLocal")
    private final WeakListenerHolder holder = new WeakListenerHolder();

    private final ObjectProperty<Step> step = new SimpleObjectProperty<>();

    private TaskExecutor browserTaskExecutor;
    private TaskExecutor deviceTaskExecutor;

    private final JFXButton btnLogin;
    private final SpinnerPane loginButtonSpinner;

    public MicrosoftAccountLoginPane() {
        this(false);
    }

    public MicrosoftAccountLoginPane(boolean bodyonly) {
        this(null, null, null, bodyonly);
    }

    public MicrosoftAccountLoginPane(Account account, Consumer<AuthInfo> callback, Runnable onCancel, boolean bodyonly) {
        this.accountToRelogin = account;
        this.loginCallback = callback;
        this.cancelCallback = onCancel;

        getStyleClass().add("microsoft-login-dialog");
        if (bodyonly) {
            this.pseudoClassStateChanged(PseudoClass.getPseudoClass("bodyonly"), true);
        } else {
            Label heading = new Label(accountToRelogin != null ? i18n("account.login.refresh") : i18n("account.create.microsoft"));
            heading.getStyleClass().add("header-label");
            setHeading(heading);
        }

        this.setMaxWidth(650);

        onEscPressed(this, this::onCancel);

        btnLogin = new JFXButton(i18n("account.login"));
        btnLogin.getStyleClass().add("dialog-accept");

        loginButtonSpinner = new SpinnerPane();
        loginButtonSpinner.getStyleClass().add("small-spinner-pane");
        loginButtonSpinner.setContent(btnLogin);

        JFXButton btnCancel = new JFXButton(i18n("button.cancel"));
        btnCancel.getStyleClass().add("dialog-cancel");
        btnCancel.setOnAction(e -> onCancel());

        setActions(loginButtonSpinner, btnCancel);

        holder.registerWeak(Accounts.OAUTH_CALLBACK.onOpenBrowserAuthorizationCode, event -> Platform.runLater(() -> {
            if (step.get() instanceof Step.StartAuthorizationCodeLogin)
                step.set(new Step.WaitForOpenBrowser(event.getUrl()));
        }));

        holder.registerWeak(Accounts.OAUTH_CALLBACK.onGrantDeviceCode, event -> Platform.runLater(() -> {
            if (step.get() instanceof Step.StartDeviceCodeLogin)
                step.set(new Step.WaitForScanQrCode(event.getUserCode(), event.getVerificationUri()));
        }));

        holder.registerWeak(Accounts.OAUTH_CALLBACK.onLoginCompletedDeviceCode, event -> Platform.runLater(() -> {
            if (step.get() instanceof Step.WaitForScanQrCode)
                step.set(new Step.DeviceLoginCompleted());
        }));

        this.step.set(Accounts.OAUTH_CALLBACK.getClientId().isEmpty()
                ? new Step.Init()
                : new Step.StartDeviceCodeLogin());
        FXUtils.onChangeAndOperate(step, this::onStep);
    }

    private void onStep(Step currentStep) {
        VBox rootContainer = new VBox(12);
        setBody(rootContainer);
        rootContainer.setAlignment(Pos.TOP_CENTER);

        if (Accounts.OAUTH_CALLBACK.getClientId().isEmpty()) {
            var snapshotHint = new HintPane(MessageDialogPane.MessageType.WARNING);
            snapshotHint.setSegment(i18n("account.methods.microsoft.snapshot"));
            rootContainer.getChildren().add(snapshotHint);
            btnLogin.setDisable(true);
            loginButtonSpinner.setLoading(false);
            return;
        }

        if (!IntegrityChecker.isOfficial()) {
            var unofficialHintPane = new HintPane(MessageDialogPane.MessageType.WARNING);
            unofficialHintPane.setSegment(i18n("unofficial.hint"));
            rootContainer.getChildren().add(unofficialHintPane);
        }

        if (currentStep instanceof Step.Init) {
            btnLogin.setOnAction(e -> this.step.set(new Step.StartDeviceCodeLogin()));
            loginButtonSpinner.setLoading(false);

            var hintPane = new HintPane(MessageDialogPane.MessageType.INFO);
            hintPane.setText(i18n("account.methods.microsoft.hint"));
            rootContainer.getChildren().add(hintPane);
        } else if (currentStep instanceof Step.StartAuthorizationCodeLogin) {
            loginButtonSpinner.setLoading(false);
            cancelAllTasks();

            VBox loadingBox = new VBox(12);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(24));
            JFXSpinner spinner = new JFXSpinner();
            Label lblLoading = new Label("正在啟動瀏覽器微軟授權登入，請稍候...");
            lblLoading.setStyle("-fx-text-fill: -monet-on-surface; -fx-font-size: 13px;");
            loadingBox.getChildren().setAll(spinner, lblLoading);

            rootContainer.getChildren().add(loadingBox);

            browserTaskExecutor = Task.supplyAsync(() -> Accounts.FACTORY_MICROSOFT.create(null, null, null, null, OAuth.GrantFlow.AUTHORIZATION_CODE))
                    .whenComplete(Schedulers.javafx(), this::onLoginCompleted)
                    .executor(true);
        } else if (currentStep instanceof Step.StartDeviceCodeLogin) {
            loginButtonSpinner.setLoading(false);
            cancelAllTasks();

            VBox loadingBox = new VBox(12);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(24));
            JFXSpinner spinner = new JFXSpinner();
            Label lblLoading = new Label("正在連接微軟登入伺服器，請稍候...");
            lblLoading.setStyle("-fx-text-fill: -monet-on-surface; -fx-font-size: 13px;");
            loadingBox.getChildren().setAll(spinner, lblLoading);

            rootContainer.getChildren().add(loadingBox);

            deviceTaskExecutor = Task.supplyAsync(() -> Accounts.FACTORY_MICROSOFT.create(null, null, null, null, OAuth.GrantFlow.DEVICE))
                    .whenComplete(Schedulers.javafx(), this::onLoginCompleted)
                    .executor(true);
        } else if (currentStep instanceof Step.WaitForOpenBrowser wait) {
            btnLogin.setOnAction(e -> {
                FXUtils.openLink(wait.url());
                loginButtonSpinner.setLoading(true);
            });
            loginButtonSpinner.setLoading(false);

            HintPane hintPane = new HintPane(MessageDialogPane.MessageType.INFO);
            hintPane.setSegment(
                    i18n("account.methods.microsoft.methods.browser.hint", StringUtils.escapeXmlAttribute(wait.url()), wait.url()),
                    FXUtils::copyText
            );

            JFXButton switchDeviceBtn = new JFXButton("📱 切換至裝置代碼登入 (https://microsoft.com/link)");
            switchDeviceBtn.getStyleClass().add("dialog-cancel");
            switchDeviceBtn.setOnAction(e -> this.step.set(new Step.StartDeviceCodeLogin()));

            rootContainer.getChildren().addAll(hintPane, switchDeviceBtn);
        } else if (currentStep instanceof Step.WaitForScanQrCode wait) {
            loginButtonSpinner.setLoading(true);

            String scanUri = "https://www.microsoft.com/link".equals(wait.verificationUri())
                    ? "https://www.microsoft.com/link?otc=" + wait.userCode()
                    : wait.verificationUri();

            var deviceHint = new HintPane(MessageDialogPane.MessageType.INFO);
            deviceHint.setSegment(i18n("account.methods.microsoft.methods.device.hint",
                    StringUtils.escapeXmlAttribute(scanUri),
                    wait.verificationUri(),
                    wait.userCode()
            ));

            var qrCode = new SVGPath();
            qrCode.fillProperty().bind(Themes.colorSchemeProperty().getPrimary());
            qrCode.setContent(QrCodeUtils.toSVGPath(QrCode.encodeText(scanUri, QrCode.Ecc.MEDIUM)));
            qrCode.setScaleX(3);
            qrCode.setScaleY(3);

            var lblCode = new Label(wait.userCode());
            lblCode.getStyleClass().add("code-label");
            lblCode.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -monet-primary; -fx-font-family: \"" + Lang.requireNonNullElse(settings().logFontFamilyProperty().get(), FXUtils.DEFAULT_MONOSPACE_FONT) + "\";");

            var codeBox = new StackPane(lblCode);
            codeBox.getStyleClass().add("code-box");
            codeBox.setStyle("-fx-padding: 8 16; -fx-background-color: -monet-surface-container-high; -fx-background-radius: 8px; -fx-border-color: -monet-outline-variant; -fx-border-radius: 8px;");
            codeBox.setCursor(Cursor.HAND);
            FXUtils.onClicked(codeBox, () -> FXUtils.copyText(wait.userCode()));
            codeBox.setMaxWidth(USE_PREF_SIZE);

            JFXButton openLinkBtn = new JFXButton("🚀 打開微軟登入頁面 (https://microsoft.com/link)");
            openLinkBtn.getStyleClass().add("dialog-accept");
            openLinkBtn.setStyle("-fx-background-color: -monet-primary; -fx-text-fill: -monet-on-primary; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6 16;");
            openLinkBtn.setOnAction(e -> {
                FXUtils.copyText(wait.userCode());
                FXUtils.openLink(scanUri);
            });

            rootContainer.getChildren().addAll(deviceHint, openLinkBtn, new Group(qrCode), codeBox);
        } else if (currentStep instanceof Step.DeviceLoginCompleted) {
            loginButtonSpinner.setLoading(true);

            var hintPane = new HintPane(MessageDialogPane.MessageType.INFO);
            hintPane.setSegment(i18n("account.methods.microsoft.methods.device.hint.completed"));

            rootContainer.getChildren().addAll(hintPane);
        } else if (currentStep instanceof Step.LoginFailed failed) {
            btnLogin.setOnAction(e -> this.step.set(new Step.StartDeviceCodeLogin()));
            loginButtonSpinner.setLoading(false);
            cancelAllTasks();

            HintPane errHintPane = new HintPane(MessageDialogPane.MessageType.ERROR);
            errHintPane.setSegment(failed.message());
            rootContainer.getChildren().add(errHintPane);
        }

        setBody(rootContainer);
    }

    private void cancelAllTasks() {
        if (browserTaskExecutor != null) browserTaskExecutor.cancel();
        if (deviceTaskExecutor != null) deviceTaskExecutor.cancel();
    }

    private void onCancel() {
        cancelAllTasks();
        if (cancelCallback != null) cancelCallback.run();
        fireEvent(new DialogCloseEvent());
    }

    private void onLoginCompleted(MicrosoftAccount account, Exception exception) {
        if (exception == null) {
            boolean storageReadOnly = accountToRelogin != null
                    ? Accounts.isAccountFilesReadOnly(accountToRelogin)
                    : Accounts.isAccountFilesReadOnly(account);
            if (storageReadOnly) {
                Controllers.confirmBackupAndOverwrite(i18n("account.storage.read_only"), () -> {
                    Accounts.forceOverwriteAccountFiles(accountToRelogin != null ? accountToRelogin : account);
                    completeLogin(account);
                });
                return;
            }

            completeLogin(account);
        } else if (!(exception instanceof CancellationException)) {
            this.step.set(new Step.LoginFailed(Accounts.localizeErrorMessage(exception)));
        }
    }

    /// Adds the logged-in account, selects it, and completes the login callback.
    private void completeLogin(MicrosoftAccount account) {
        if (accountToRelogin != null) Accounts.getAccounts().remove(accountToRelogin);

        int oldIndex = Accounts.getAccounts().indexOf(account);
        if (oldIndex == -1) {
            Accounts.getAccounts().add(account);
        } else {
            Accounts.getAccounts().remove(oldIndex);
            Accounts.getAccounts().add(oldIndex, account);
        }

        Accounts.setSelectedAccount(account);

        if (loginCallback != null) {
            try {
                loginCallback.accept(account.logIn());
            } catch (AuthenticationException e) {
                this.step.set(new Step.LoginFailed(Accounts.localizeErrorMessage(e)));
                return;
            }
        }
        fireEvent(new DialogCloseEvent());
    }

    private sealed interface Step {
        final class Init implements Step {
        }

        final class StartAuthorizationCodeLogin implements Step {
        }

        record WaitForOpenBrowser(String url) implements Step {

        }

        final class StartDeviceCodeLogin implements Step {
        }

        record WaitForScanQrCode(String userCode, String verificationUri) implements Step {

        }

        record DeviceLoginCompleted() implements Step {

        }

        record LoginFailed(String message) implements Step {
        }
    }

}
