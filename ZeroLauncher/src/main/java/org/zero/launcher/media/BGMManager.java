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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.zero.launcher.setting.GameDirectoryManager;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BGMManager {

    public record AudioTrack(String name, String author, String hash, Path localPath) {
        @Override
        public String toString() {
            return author + " - " + name;
        }
    }

    // Classic C418 Minecraft Soundtracks with official Mojang Asset Hashes
    private static final List<AudioTrack> DEFAULT_TRACKS = List.of(
            new AudioTrack("Sweden", "C418", "be9b14926936c4509f7fe5002cdfbba3b2752d82", null),
            new AudioTrack("Wet Hands", "C418", "e8a73969b515a15df138a4d55b07f4ff573db25f", null),
            new AudioTrack("Subwoofer Lullaby", "C418", "0dbcd56e6d8a931818f0913a0e23f784106d4ad8", null),
            new AudioTrack("Haggstrom", "C418", "c25705e16f1d51a56a2b0a4604250b3844ceadcf", null),
            new AudioTrack("Living Mice", "C418", "532fdae2fd43bc046f78a1b7b79f28161f25fc4c", null),
            new AudioTrack("Mice on Venus", "C418", "06a2f38a094573652977232abcc4f268cf6745ad", null)
    );

    private static final BGMManager INSTANCE = new BGMManager();

    public static BGMManager getInstance() {
        return INSTANCE;
    }

    private final List<AudioTrack> playlist = new ArrayList<>();
    private int currentTrackIndex = 0;

    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final StringProperty currentTrackTitle = new SimpleStringProperty("C418 - Sweden");
    private final DoubleProperty volume = new SimpleDoubleProperty(0.38); // Gentle, comfortable ambient level

    private final AtomicBoolean pausedByGame = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean isPlayingThread = new AtomicBoolean(false);

    private SourceDataLine currentLine;
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ZeroLauncher-BGM-Player");
        t.setDaemon(true);
        return t;
    });

    private BGMManager() {
        initPlaylist();
    }

    private void initPlaylist() {
        playlist.clear();
        for (AudioTrack def : DEFAULT_TRACKS) {
            Path found = findAssetFile(def.hash());
            playlist.add(new AudioTrack(def.name(), def.author(), def.hash(), found));
        }

        // Also check custom bgm/ folder in current dir
        try {
            Path bgmDir = Paths.get("bgm");
            if (Files.isDirectory(bgmDir)) {
                try (var stream = Files.list(bgmDir)) {
                    stream.filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".ogg") || name.endsWith(".mp3") || name.endsWith(".wav");
                    }).forEach(p -> {
                        String rawName = p.getFileName().toString();
                        int dot = rawName.lastIndexOf('.');
                        String base = dot > 0 ? rawName.substring(0, dot) : rawName;
                        playlist.add(new AudioTrack(base, "Custom", "", p));
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Path findAssetFile(String hash) {
        if (hash == null || hash.length() < 2) return null;
        String prefix = hash.substring(0, 2);

        List<Path> candidateRoots = new ArrayList<>();
        candidateRoots.add(Paths.get(".minecraft", "assets", "objects", prefix, hash));
        candidateRoots.add(Paths.get("run", "assets", "objects", prefix, hash));
        candidateRoots.add(Paths.get("assets", "objects", prefix, hash));

        try {
            String appdata = System.getenv("APPDATA");
            if (appdata != null) {
                candidateRoots.add(Paths.get(appdata, ".minecraft", "assets", "objects", prefix, hash));
            }
        } catch (Exception ignored) {
        }

        try {
            var repo = GameDirectoryManager.getSelectedRepository();
            if (repo != null && repo.getGameDirectory() != null && repo.getGameDirectory().getPath() != null) {
                Path dir = repo.getGameDirectory().getPath().toPath();
                candidateRoots.add(dir.resolve("assets/objects/" + prefix + "/" + hash));
            }
        } catch (Exception ignored) {
        }

        for (Path p : candidateRoots) {
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath();
            }
        }
        return null;
    }

    public synchronized void play() {
        if (playing.get()) return;
        stopRequested.set(false);
        playTrack(currentTrackIndex);
    }

    public synchronized void pause() {
        stopRequested.set(true);
        stopCurrentLine();
        Platform.runLater(() -> playing.set(false));
    }

    public synchronized void toggle() {
        if (playing.get()) {
            pause();
        } else {
            play();
        }
    }

    public synchronized void nextTrack() {
        if (playlist.isEmpty()) return;
        int next = (currentTrackIndex + 1) % playlist.size();
        playTrack(next);
    }

    public synchronized void prevTrack() {
        if (playlist.isEmpty()) return;
        int prev = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
        playTrack(prev);
    }

    public synchronized void playTrack(int index) {
        if (playlist.isEmpty()) return;
        currentTrackIndex = Math.max(0, Math.min(index, playlist.size() - 1));
        AudioTrack track = playlist.get(currentTrackIndex);

        stopRequested.set(true);
        stopCurrentLine();

        Platform.runLater(() -> {
            currentTrackTitle.set(track.toString());
            playing.set(true);
        });

        audioExecutor.submit(() -> streamTrack(track));
    }

    private void stopCurrentLine() {
        try {
            if (currentLine != null) {
                currentLine.stop();
                currentLine.flush();
                currentLine.close();
                currentLine = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void streamTrack(AudioTrack track) {
        stopRequested.set(false);
        isPlayingThread.set(true);

        InputStream rawStream = null;
        AudioInputStream din = null;

        try {
            Path file = track.localPath();
            if (file == null || !Files.isRegularFile(file)) {
                // Try to find file again
                file = findAssetFile(track.hash());
            }

            if (file != null && Files.isRegularFile(file)) {
                rawStream = new BufferedInputStream(Files.newInputStream(file));
            } else if (track.hash() != null && !track.hash().isEmpty()) {
                // Stream from Mojang CDN
                String prefix = track.hash().substring(0, 2);
                String url = "https://resources.download.minecraft.net/" + prefix + "/" + track.hash();
                rawStream = new BufferedInputStream(URI.create(url).toURL().openStream());
            }

            org.zero.launcher.util.logging.Logger.LOG.info("BGMManager: starting playback for " + track.name() + " (" + file + ")");

            AudioInputStream in;
            if (file != null && Files.isRegularFile(file)) {
                in = AudioSystem.getAudioInputStream(file.toFile());
            } else if (rawStream != null) {
                in = AudioSystem.getAudioInputStream(rawStream);
            } else {
                org.zero.launcher.util.logging.Logger.LOG.warning("BGMManager: Track not available: " + track.name());
                Platform.runLater(() -> playing.set(false));
                return;
            }

            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(decodedFormat);
            line.start();

            synchronized (this) {
                this.currentLine = line;
                applyVolume(line, volume.get());
            }

            byte[] buffer = new byte[4096];
            int nBytesRead;
            while (!stopRequested.get() && (nBytesRead = din.read(buffer, 0, buffer.length)) != -1) {
                line.write(buffer, 0, nBytesRead);
                applyVolume(line, volume.get());
            }

            line.drain();
            line.stop();
            line.close();

            if (!stopRequested.get()) {
                // Track finished naturally, play next
                Platform.runLater(this::nextTrack);
            }
        } catch (Exception e) {
            org.zero.launcher.util.logging.Logger.LOG.warning("BGMManager playback failed for " + track.name(), e);
            Platform.runLater(() -> playing.set(false));
        } finally {
            try {
                if (din != null) din.close();
                if (rawStream != null) rawStream.close();
            } catch (Exception ignored) {
            }
            isPlayingThread.set(false);
        }
    }

    public synchronized void pauseForGame() {
        if (playing.get()) {
            pausedByGame.set(true);
            pause();
        }
    }

    public synchronized void resumeFromGame() {
        if (pausedByGame.getAndSet(false)) {
            play();
        }
    }

    public synchronized void setVolume(double vol) {
        this.volume.set(Math.max(0.0, Math.min(1.0, vol)));
        if (currentLine != null) {
            applyVolume(currentLine, this.volume.get());
        }
    }

    private void applyVolume(SourceDataLine line, double vol) {
        try {
            if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                if (vol <= 0.001) {
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    float dB = (float) (Math.log10(vol) * 20.0);
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean en) {
        this.enabled.set(en);
        if (!en) {
            pause();
        } else {
            play();
        }
    }

    public StringProperty currentTrackTitleProperty() {
        return currentTrackTitle;
    }

    public String getCurrentTrackTitle() {
        return currentTrackTitle.get();
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public double getVolume() {
        return volume.get();
    }
}
