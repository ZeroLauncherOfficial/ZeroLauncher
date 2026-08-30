/*
 * ZeroLauncher
 * Copyright (C) 2026 Zero <Zero@zerolauncher.net> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.zero.launcher.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.zero.launcher.util.platform.OperatingSystem;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.zero.launcher.util.logging.Logger.LOG;

/// Manages Discord Rich Presence (RPC) IPC integration.
@NotNullByDefault
public final class DiscordRPCManager {

    private static final String DEFAULT_CLIENT_ID = "1345112233445566778"; // Zero Launcher Discord App ID
    private static final int OPCODE_HANDSHAKE = 0;
    private static final int OPCODE_FRAME = 1;
    private static final int OPCODE_CLOSE = 2;

    private static final DiscordRPCManager INSTANCE = new DiscordRPCManager();

    public static DiscordRPCManager getInstance() {
        return INSTANCE;
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ZeroLauncher-DiscordRPC");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong launcherStartTime = new AtomicLong(System.currentTimeMillis() / 1000L);

    private @Nullable ByteChannel ipcChannel = null;
    private @Nullable RandomAccessFile winPipeFile = null;

    private final AtomicReference<Activity> currentActivity = new AtomicReference<>();

    public record Activity(
            @Nullable String details,
            @Nullable String state,
            long startTimestamp,
            @Nullable String largeImageKey,
            @Nullable String largeImageText,
            @Nullable String smallImageKey,
            @Nullable String smallImageText,
            @Nullable String button1Label,
            @Nullable String button1Url
    ) {
    }

    private DiscordRPCManager() {
        // Default launcher activity
        updateInLauncher();
    }

    public void start() {
        if (!enabled.get()) return;

        executor.scheduleWithFixedDelay(() -> {
            try {
                if (!connected.get()) {
                    tryConnect();
                } else {
                    // Send periodic heartbeat or update if activity changed
                    sendCurrentActivity();
                }
            } catch (Exception e) {
                closeConnection();
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.execute(this::closeConnection);
        executor.shutdown();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enable) {
        this.enabled.set(enable);
        if (!enable) {
            executor.execute(this::closeConnection);
        } else {
            executor.execute(this::tryConnect);
        }
    }

    public void updateInLauncher() {
        Activity act = new Activity(
                "在啟動器中 (Zero Launcher)",
                "挑選遊戲實例中 · v1.0.0",
                launcherStartTime.get(),
                "logo",
                "Zero Launcher",
                "idle",
                "待命中",
                "取得 Zero Launcher",
                "https://github.com/ZeroLauncherOfficial/ZeroLauncher"
        );
        currentActivity.set(act);
        if (connected.get()) {
            executor.execute(this::sendCurrentActivity);
        }
    }

    public void updatePlayingGame(@Nullable String instanceName, @Nullable String mcVersion, @Nullable String loaderName) {
        String details = (instanceName != null && !instanceName.isBlank()) ? "正在遊玩 " + instanceName : "正在遊玩 Minecraft";
        String ver = (mcVersion != null) ? mcVersion : "";
        String loader = (loaderName != null) ? loaderName : "";
        String state = (!ver.isBlank()) ? "Minecraft " + ver + (!loader.isBlank() ? " (" + loader + ")" : "") : "Minecraft";
        String smallKey = loader.toLowerCase().contains("fabric") ? "fabric"
                : loader.toLowerCase().contains("forge") ? "forge"
                : loader.toLowerCase().contains("neoforge") ? "neoforge"
                : loader.toLowerCase().contains("quilt") ? "quilt"
                : "vanilla";

        Activity act = new Activity(
                details,
                state,
                System.currentTimeMillis() / 1000L,
                "logo",
                "Zero Launcher v1.0.0",
                smallKey,
                loader.isBlank() ? "Vanilla" : loader,
                "取得 Zero Launcher",
                "https://github.com/ZeroLauncherOfficial/ZeroLauncher"
        );
        currentActivity.set(act);
        if (connected.get()) {
            executor.execute(this::sendCurrentActivity);
        }
    }

    private synchronized void tryConnect() {
        if (connected.get() || !enabled.get()) return;

        closeConnection();

        try {
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
                for (int i = 0; i < 10; i++) {
                    try {
                        RandomAccessFile raf = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                        FileChannel ch = raf.getChannel();
                        if (performHandshake(ch)) {
                            this.winPipeFile = raf;
                            this.ipcChannel = ch;
                            this.connected.set(true);
                            startResponseReader(ch);
                            LOG.info("DiscordRPC: Connected to named pipe discord-ipc-" + i);
                            sendCurrentActivity();
                            return;
                        } else {
                            raf.close();
                        }
                    } catch (Exception ignored) {
                    }
                }
            } else {
                // Unix / macOS domain sockets
                String xdg = System.getenv("XDG_RUNTIME_DIR");
                String tmp = System.getenv("TMPDIR");
                if (tmp == null) tmp = "/tmp";

                String[] candidateDirs = {
                        xdg,
                        tmp,
                        tmp + "/app/com.discordapp.Discord",
                        "/tmp"
                };

                for (String dir : candidateDirs) {
                    if (dir == null || dir.isBlank()) continue;
                    for (int i = 0; i < 10; i++) {
                        Path socketPath = Paths.get(dir, "discord-ipc-" + i);
                        if (Files.exists(socketPath)) {
                            try {
                                SocketChannel sc = SocketChannel.open(StandardProtocolFamily.UNIX);
                                sc.connect(UnixDomainSocketAddress.of(socketPath));
                                if (performHandshake(sc)) {
                                    this.ipcChannel = sc;
                                    this.connected.set(true);
                                    startResponseReader(sc);
                                    LOG.info("DiscordRPC: Connected to socket " + socketPath);
                                    sendCurrentActivity();
                                    return;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            closeConnection();
        }
    }

    private void startResponseReader(ByteChannel channel) {
        Thread readerThread = new Thread(() -> {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            try {
                while (connected.get() && channel.isOpen()) {
                    header.clear();
                    while (header.hasRemaining()) {
                        if (channel.read(header) == -1) {
                            closeConnection();
                            return;
                        }
                    }
                    header.flip();
                    int op = header.getInt();
                    int len = header.getInt();
                    if (len > 0) {
                        ByteBuffer body = ByteBuffer.allocate(len);
                        while (body.hasRemaining()) {
                            if (channel.read(body) == -1) {
                                closeConnection();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                closeConnection();
            }
        }, "DiscordRPC-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private @Nullable String customClientId = null;

    public void setCustomClientId(@Nullable String clientId) {
        this.customClientId = clientId;
        if (connected.get()) {
            executor.execute(() -> {
                closeConnection();
                tryConnect();
            });
        }
    }

    public String getClientId() {
        return (customClientId != null && !customClientId.isBlank()) ? customClientId : DEFAULT_CLIENT_ID;
    }

    private boolean performHandshake(ByteChannel channel) {
        try {
            JsonObject handshake = new JsonObject();
            handshake.addProperty("v", 1);
            handshake.addProperty("client_id", getClientId());
            writePacket(channel, OPCODE_HANDSHAKE, handshake.toString());

            // Read response
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            while (header.hasRemaining()) {
                if (channel.read(header) == -1) return false;
            }
            header.flip();
            int op = header.getInt();
            int len = header.getInt();

            if (len > 0 && len < 65536) {
                ByteBuffer body = ByteBuffer.allocate(len);
                while (body.hasRemaining()) {
                    if (channel.read(body) == -1) return false;
                }
                body.flip();
                String resp = StandardCharsets.UTF_8.decode(body).toString();
                JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
                return obj.has("cmd") && "DISPATCH".equals(obj.get("cmd").getAsString());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized void sendCurrentActivity() {
        if (!connected.get() || ipcChannel == null) return;
        Activity act = currentActivity.get();
        if (act == null) return;

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");

            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());

            JsonObject activity = new JsonObject();
            if (act.details() != null) activity.addProperty("details", act.details());
            if (act.state() != null) activity.addProperty("state", act.state());

            if (act.startTimestamp() > 0) {
                JsonObject timestamps = new JsonObject();
                timestamps.addProperty("start", act.startTimestamp());
                activity.add("timestamps", timestamps);
            }

            JsonObject assets = new JsonObject();
            if (act.largeImageKey() != null) assets.addProperty("large_image", act.largeImageKey());
            if (act.largeImageText() != null) assets.addProperty("large_text", act.largeImageText());
            if (act.smallImageKey() != null) assets.addProperty("small_image", act.smallImageKey());
            if (act.smallImageText() != null) assets.addProperty("small_text", act.smallImageText());
            activity.add("assets", assets);

            if (act.button1Label() != null && act.button1Url() != null) {
                JsonArray buttons = new JsonArray();
                JsonObject btn = new JsonObject();
                btn.addProperty("label", act.button1Label());
                btn.addProperty("url", act.button1Url());
                buttons.add(btn);
                activity.add("buttons", buttons);
            }

            args.add("activity", activity);
            payload.add("args", args);
            payload.addProperty("nonce", UUID.randomUUID().toString());

            writePacket(ipcChannel, OPCODE_FRAME, payload.toString());
        } catch (Exception e) {
            closeConnection();
        }
    }

    private void writePacket(ByteChannel channel, int opcode, String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(bytes.length);
        buf.put(bytes);
        buf.flip();

        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    private synchronized void closeConnection() {
        connected.set(false);
        try {
            if (ipcChannel != null) {
                ipcChannel.close();
                ipcChannel = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (winPipeFile != null) {
                winPipeFile.close();
                winPipeFile = null;
            }
        } catch (Exception ignored) {
        }
    }
}
