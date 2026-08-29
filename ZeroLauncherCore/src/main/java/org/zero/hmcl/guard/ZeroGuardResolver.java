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
package org.zero.hmcl.guard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.zero.hmcl.game.GameRepository;
import org.zero.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@NotNullByDefault
public final class ZeroGuardResolver {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static CompletableFuture<Boolean> resolveAndInstall(
            GameRepository repository,
            String versionId,
            String gameVersion,
            String loaderType,
            DependencyIssue issue) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetId = issue.getTargetModId().toLowerCase(Locale.ROOT);
                String slug = mapModIdToModrinthSlug(targetId);
                String loader = mapLoaderType(loaderType);

                Logger.LOG.info("ZeroGuardResolver: Resolving missing dependency [" + targetId + "] (slug: " + slug + ") for MC " + gameVersion + " (" + loader + ")");

                String queryUrl = "https://api.modrinth.com/v2/project/" + URLEncoder.encode(slug, StandardCharsets.UTF_8)
                        + "/version?game_versions=%5B%22" + URLEncoder.encode(gameVersion, StandardCharsets.UTF_8) + "%22%5D"
                        + "&loaders=%5B%22" + URLEncoder.encode(loader, StandardCharsets.UTF_8) + "%22%5D";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(queryUrl))
                        .header("User-Agent", "ZeroLauncher/1.0.0 (contact@zerolauncher.net)")
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    Logger.LOG.warning("ZeroGuardResolver: Modrinth API returned status " + response.statusCode() + " for " + slug);
                    return false;
                }

                JsonElement parsed = JsonParser.parseString(response.body());
                if (!parsed.isJsonArray()) {
                    return false;
                }

                JsonArray versionsArray = parsed.getAsJsonArray();
                if (versionsArray.isEmpty()) {
                    Logger.LOG.warning("ZeroGuardResolver: No matching version found on Modrinth for " + slug + " on MC " + gameVersion);
                    return false;
                }

                JsonObject targetVersion = versionsArray.get(0).getAsJsonObject();
                JsonArray filesArray = targetVersion.getAsJsonArray("files");
                if (filesArray == null || filesArray.isEmpty()) {
                    return false;
                }

                JsonObject fileObj = filesArray.get(0).getAsJsonObject();
                for (JsonElement f : filesArray) {
                    JsonObject fo = f.getAsJsonObject();
                    if (fo.has("primary") && fo.get("primary").getAsBoolean()) {
                        fileObj = fo;
                        break;
                    }
                }

                String downloadUrl = fileObj.get("url").getAsString();
                String fileName = fileObj.get("filename").getAsString();

                Path modsDir = repository.getModsDirectory(versionId);
                if (!Files.exists(modsDir)) {
                    Files.createDirectories(modsDir);
                }

                Path targetFile = modsDir.resolve(fileName);
                Logger.LOG.info("ZeroGuardResolver: Downloading " + fileName + " from " + downloadUrl);

                HttpRequest dlRequest = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("User-Agent", "ZeroLauncher/1.0.0 (contact@zerolauncher.net)")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<InputStream> dlResponse = HTTP_CLIENT.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (dlResponse.statusCode() == 200) {
                    try (InputStream is = dlResponse.body()) {
                        Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Logger.LOG.info("ZeroGuardResolver: Successfully installed " + fileName + " to " + targetFile);
                    return true;
                } else {
                    Logger.LOG.warning("ZeroGuardResolver: Download failed with status " + dlResponse.statusCode());
                    return false;
                }
            } catch (Throwable e) {
                Logger.LOG.warning("ZeroGuardResolver: Exception resolving dependency: " + e.getMessage());
                return false;
            }
        });
    }

    private static String mapModIdToModrinthSlug(String modId) {
        String lower = modId.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "fabric-api", "fabric" -> "fabric-api";
            case "cloth-config", "cloth-config2", "cloth_config" -> "cloth-config";
            case "architectury", "architectury-api" -> "architectury-api";
            case "jei" -> "jei";
            case "rei", "roughlyenoughitems" -> "roughly-enough-items";
            case "emi" -> "emi";
            case "sodium" -> "sodium";
            case "iris" -> "iris";
            case "indium" -> "indium";
            case "modmenu" -> "modmenu";
            case "geckolib" -> "geckolib";
            case "citresewn" -> "cit-resewn";
            case "curios" -> "curios";
            case "trinkets" -> "trinkets";
            case "appleskin" -> "appleskin";
            default -> modId;
        };
    }

    private static String mapLoaderType(String loaderType) {
        String lower = loaderType.toLowerCase(Locale.ROOT);
        if (lower.contains("fabric")) return "fabric";
        if (lower.contains("quilt")) return "quilt";
        if (lower.contains("neoforge")) return "neoforge";
        if (lower.contains("forge")) return "forge";
        return "fabric";
    }
}
