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
package org.zero.launcher.game;

import org.zero.launcher.setting.GpuPreference;
import org.zero.launcher.util.platform.OperatingSystem;
import org.zero.launcher.util.platform.hardware.GraphicsCard;
import org.zero.launcher.util.platform.hardware.HardwareVendor;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.zero.launcher.util.logging.Logger.LOG;

/**
 * <h1>GpuDetector — 智慧顯示卡偵測與強制選卡引擎</h1>
 * <p>
 * 自動偵測本機獨立顯卡 (NVIDIA / AMD / Intel Arc) 與內建顯卡 (Intel UHD / Iris / AMD Radeon Graphics)，
 * 並在啟動遊戲時自動注入驅動級環境變數與 Windows 10/11 DirectX 圖形效能註冊表偏好。
 *
 * @author Zero
 */
public final class GpuDetector {

    private static volatile List<GraphicsCard> cachedGpuList = null;
    private static volatile Boolean cachedHasDiscreteGpu = null;
    private static volatile String cachedSummary = null;

    private GpuDetector() {
    }

    /**
     * 獲取本機所有顯卡列表
     */
    public static List<GraphicsCard> getGraphicsCards() {
        if (cachedGpuList == null) {
            try {
                List<GraphicsCard> cards = org.zero.launcher.util.platform.SystemInfo.getGraphicsCards();
                cachedGpuList = (cards != null) ? cards : List.of();
            } catch (Throwable t) {
                LOG.warning("Failed to detect graphics cards: " + t.getMessage());
                cachedGpuList = List.of();
            }
        }
        return cachedGpuList;
    }

    /**
     * 判斷某張顯卡是否為獨立顯卡
     */
    public static boolean isDiscreteGpu(GraphicsCard card) {
        if (card == null) return false;
        if (card.getType() == GraphicsCard.Type.Discrete) {
            return true;
        }

        String name = card.getName().toLowerCase(Locale.ROOT);
        HardwareVendor vendor = card.getVendor();

        if (name.contains("rtx") || name.contains("gtx") || name.contains("geforce")
                || name.contains("radeon rx") || name.contains("arc a") || name.contains("arc pro")
                || name.contains("discrete") || name.contains("quadro") || name.contains("tesla")) {
            return true;
        }

        if (name.contains("uhd") || name.contains("hd graphics") || name.contains("iris")
                || name.contains("vega") || name.contains("internal") || name.contains("integrated")
                || name.contains("microsoft basic render") || name.contains("virtualbox") || name.contains("vmware")) {
            return false;
        }

        return vendor == HardwareVendor.NVIDIA;
    }

    /**
     * 判斷本機是否存在獨立顯卡
     */
    public static boolean hasDiscreteGpu() {
        if (cachedHasDiscreteGpu == null) {
            List<GraphicsCard> cards = getGraphicsCards();
            boolean hasDiscrete = false;
            for (GraphicsCard card : cards) {
                if (isDiscreteGpu(card)) {
                    hasDiscrete = true;
                    break;
                }
            }
            cachedHasDiscreteGpu = hasDiscrete;
        }
        return cachedHasDiscreteGpu;
    }

    /**
     * 獲取顯示在 UI 上的顯卡偵測摘要資訊
     */
    public static String getGpuSummary() {
        if (cachedSummary == null) {
            List<GraphicsCard> cards = getGraphicsCards();
            if (cards.isEmpty()) {
                cachedSummary = "未偵測到相容顯示卡資訊";
            } else {
                List<String> names = new ArrayList<>();
                for (GraphicsCard card : cards) {
                    boolean discrete = isDiscreteGpu(card);
                    names.add(card.getName() + (discrete ? " (獨立顯卡)" : " (內建顯卡)"));
                }
                cachedSummary = "偵測到：" + String.join(" + ", names);
            }
        }
        return cachedSummary;
    }

    /**
     * 應用 GPU 偏好設定：注入環境變數並在 Windows 上配置 DirectX UserGpuPreferences
     *
     * @param preference       GPU 偏好設定 (AUTO / DISCRETE / INTEGRATED)
     * @param javaExecutablePath Java 執行檔路徑 (javaw.exe)
     * @param env              要注入的環境變數 Map
     */
    public static void applyGpuPreference(GpuPreference preference, String javaExecutablePath, Map<String, String> env) {
        if (preference == null) {
            preference = GpuPreference.AUTO;
        }

        boolean useDiscrete;
        if (preference == GpuPreference.DISCRETE) {
            useDiscrete = true;
        } else if (preference == GpuPreference.INTEGRATED) {
            useDiscrete = false;
        } else {
            // AUTO: 有獨顯則自動選獨顯，無獨顯選內顯
            useDiscrete = hasDiscreteGpu();
        }

        LOG.info("Applying GPU Preference: " + preference + " (Target Discrete GPU: " + useDiscrete + ")");

        // 1. 注入跨平台驅動環境變數
        if (useDiscrete) {
            env.put("DRI_PRIME", "1");
            env.put("__NV_PRIME_RENDER_OFFLOAD", "1");
            env.put("__GLX_VENDOR_LIBRARY_NAME", "nvidia");
            env.put("SHIM_MCCOMPAT", "0xD");
            env.put("GPU_FORCE_64BIT_PTR", "1");
            env.put("GPU_MAX_HEAP_SIZE", "100");
            env.put("GPU_USE_SYNC_OBJECTS", "1");
        } else {
            env.put("DRI_PRIME", "0");
            env.put("__NV_PRIME_RENDER_OFFLOAD", "0");
        }

        // 2. 在 Windows 10/11 上寫入微軟官方 DirectX 圖形效能註冊表 (UserGpuPreferences)
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && javaExecutablePath != null && !javaExecutablePath.isBlank()) {
            CompletableFuture.runAsync(() -> {
                try {
                    // GpuPreference=2 (High Performance 獨顯), GpuPreference=1 (Power Saving 內顯)
                    int prefValue = useDiscrete ? 2 : 1;
                    String regValue = "GpuPreference=" + prefValue + ";";
                    
                    ProcessBuilder pb = new ProcessBuilder(
                            "reg.exe", "add",
                            "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences",
                            "/v", javaExecutablePath,
                            "/t", "REG_SZ",
                            "/d", regValue,
                            "/f"
                    );
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor();
                    LOG.info("Configured Windows UserGpuPreferences for " + javaExecutablePath + " -> " + regValue);
                } catch (Throwable t) {
                    LOG.warning("Failed to set Windows DirectX UserGpuPreferences: " + t.getMessage());
                }
            });
        }
    }
}
