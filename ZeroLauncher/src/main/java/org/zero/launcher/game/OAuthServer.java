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
package org.zero.launcher.game;

import fi.iki.elonen.NanoHTTPD;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.zero.launcher.auth.AuthenticationException;
import org.zero.launcher.auth.OAuth;
import org.zero.launcher.event.Event;
import org.zero.launcher.event.EventManager;
import org.zero.launcher.theme.Themes;
import org.zero.launcher.util.StringUtils;
import org.zero.launcher.util.io.IOUtils;
import org.zero.launcher.util.io.JarUtils;
import org.zero.launcher.util.io.NetworkUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.zero.launcher.util.Lang.mapOf;
import static org.zero.launcher.util.Lang.thread;
import static org.zero.launcher.util.i18n.I18n.i18n;
import static org.zero.launcher.util.logging.Logger.LOG;

/// Local OAuth server that receives authentication redirects from Microsoft OAuth flow.
@NotNullByDefault
public final class OAuthServer extends NanoHTTPD implements OAuth.Session {
    private final int port;
    private final CompletableFuture<String> future = new CompletableFuture<>();
    private final String codeVerifier;
    private final String state;

    public static @Nullable String lastlyOpenedURL;

    private @Nullable String idToken;

    private OAuthServer(int port) {
        super("127.0.0.1", port);

        this.port = port;

        var encoder = Base64.getUrlEncoder().withoutPadding();
        var random = new SecureRandom();

        {
            // https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1
            // https://datatracker.ietf.org/doc/html/rfc6749#section-10.12
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            this.state = encoder.encodeToString(bytes);
        }

        {
            // https://datatracker.ietf.org/doc/html/rfc7636#section-4.1
            byte[] bytes = new byte[64];
            random.nextBytes(bytes);
            this.codeVerifier = encoder.encodeToString(bytes);
        }
    }

    @Override
    public String getCodeVerifier() {
        return codeVerifier;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getRedirectURI() {
        return String.format("http://localhost:%d/auth-response", port);
    }

    @Override
    public String waitFor() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public @Nullable String getIdToken() {
        return idToken;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (!"/auth-response".equals(session.getUri())) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }

        if (session.getMethod() != Method.GET && session.getMethod() != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method Not Allowed");
        }

        if (session.getMethod() == Method.POST) {
            Map<String, String> files = new HashMap<>();
            try {
                session.parseBody(files);
            } catch (IOException e) {
                LOG.warning("Failed to read post data", e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Server Error");
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        String parameters = session.getQueryParameterString();
        Map<String, String> query = mapOf(NetworkUtils.parseQuery(parameters));

        String stateParam = query.get("state");
        String code = query.get("code");
        String error = query.get("error");

        // Verify that this request is actually the callback from Microsoft matching our state
        if (stateParam == null || !this.state.equals(stateParam)) {
            // Unauthorized or invalid probe request. Do NOT complete future exceptionally and do NOT stop the server.
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid or missing state parameter");
        }

        if (error != null) {
            String errorDesc = query.getOrDefault("error_description", error);
            LOG.warning("OAuth authentication error: " + errorDesc);
            future.completeExceptionally(new AuthenticationException("OAuth authentication failed: " + errorDesc));
            thread(() -> {
                try {
                    Thread.sleep(1000);
                    stop();
                } catch (InterruptedException ignored) {
                }
            });
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Authentication failed: " + errorDesc);
        }

        if (code == null || code.isBlank()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing authorization code");
        }

        // Successfully received code with matching state
        idToken = query.get("id_token");
        future.complete(code);

        String html;
        try {
            html = IOUtils.readFullyAsString(OAuthServer.class.getResourceAsStream("/assets/microsoft_auth.html"))
                    .replace("%style%", Themes.getTheme().toColorScheme().toStyleSheet().replace("-monet", "--monet"))
                    .replace("%lang%", Locale.getDefault().toLanguageTag())
                    .replace("%success%", i18n("message.success"))
                    .replace("%ok%", i18n("button.ok"))
                    .replace("%close_page%", i18n("account.methods.microsoft.close_page"));
        } catch (IOException e) {
            LOG.error("Failed to load html", e);
            html = "<html><body><h1>Authentication Successful</h1><p>You can close this window now.</p></body></html>";
        }
        thread(() -> {
            try {
                Thread.sleep(1000);
                stop();
            } catch (InterruptedException ignored) {
            }
        });
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html);
    }

    @Override
    public void close() {
        if (!future.isDone())
            future.completeExceptionally(new AuthenticationException("OAuth server is closing"));
        stop();
    }

    /// Factory for creating and managing OAuth session callbacks.
    @NotNullByDefault
    public static class Factory implements OAuth.Callback {
        /// Event fired when a device code is granted.
        public final EventManager<GrantDeviceCodeEvent> onGrantDeviceCode = new EventManager<>();
        /// Event fired when login is completed via device code.
        public final EventManager<LoginCompletedDeviceCodeEvent> onLoginCompletedDeviceCode = new EventManager<>();
        /// Event fired to open browser for authorization code grant.
        public final EventManager<OpenBrowserEvent> onOpenBrowserAuthorizationCode = new EventManager<>();
        /// Event fired to open browser for device code grant.
        public final EventManager<OpenBrowserEvent> onOpenBrowserDevice = new EventManager<>();

        @Override
        public OAuth.Session startServer() throws IOException, AuthenticationException {
            if (StringUtils.isBlank(getClientId())) {
                throw new MicrosoftAuthenticationNotSupportedException();
            }

            @Nullable IOException exception = null;
            for (int port : new int[]{29111, 29112, 29113, 29114, 29115}) {
                try {
                    OAuthServer server = new OAuthServer(port);
                    server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
                    return server;
                } catch (IOException e) {
                    exception = e;
                }
            }
            if (exception != null) {
                throw exception;
            }
            throw new IOException("Failed to bind OAuth server to any port");
        }

        @Override
        public void grantDeviceCode(String userCode, String verificationURI) {
            onGrantDeviceCode.fireEvent(new GrantDeviceCodeEvent(this, userCode, verificationURI));
        }

        @Override
        public void loginCompletedDeviceCode() {
            onLoginCompletedDeviceCode.fireEvent(new LoginCompletedDeviceCodeEvent(this));
        }

        @Override
        public void openBrowser(OAuth.GrantFlow grantFlow, String url) throws IOException {
            lastlyOpenedURL = url;

            switch (grantFlow) {
                case AUTHORIZATION_CODE -> onOpenBrowserAuthorizationCode.fireEvent(new OpenBrowserEvent(this, url));
                case DEVICE -> onOpenBrowserDevice.fireEvent(new OpenBrowserEvent(this, url));
            }
        }

        @Override
        public String getClientId() {
            String id = System.getProperty("zero.microsoft.auth.id",
                    JarUtils.getAttribute("zero.microsoft.auth.id", ""));
            if (id == null || id.isEmpty()) {
                return "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";
            }
            return id;
        }
    }

    /// Event representing device code grant.
    @NotNullByDefault
    public static class GrantDeviceCodeEvent extends Event {
        private final String userCode;
        private final String verificationUri;

        /// Creates a new GrantDeviceCodeEvent.
        public GrantDeviceCodeEvent(Object source, String userCode, String verificationUri) {
            super(source);
            this.userCode = userCode;
            this.verificationUri = verificationUri;
        }

        /// Returns the user code.
        public String getUserCode() {
            return userCode;
        }

        /// Returns the verification URI.
        public String getVerificationUri() {
            return verificationUri;
        }
    }

    /// Event representing completion of device code login.
    @NotNullByDefault
    public static class LoginCompletedDeviceCodeEvent extends Event {
        /// Creates a new LoginCompletedDeviceCodeEvent.
        public LoginCompletedDeviceCodeEvent(Object source) {
            super(source);
        }
    }

    /// Event representing browser opening request.
    @NotNullByDefault
    public static class OpenBrowserEvent extends Event {
        private final String url;

        /// Creates a new OpenBrowserEvent.
        public OpenBrowserEvent(Object source, String url) {
            super(source);
            this.url = url;
        }

        /// Returns the URL to open.
        public String getUrl() {
            return url;
        }
    }

    /// Exception thrown when Microsoft authentication is not supported.
    @NotNullByDefault
    public static class MicrosoftAuthenticationNotSupportedException extends AuthenticationException {
    }
}
