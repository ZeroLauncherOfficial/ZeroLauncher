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
package org.zero.launcher.ui.animation;

import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1>PclAnimationEngine — PCL2 風格自研物理動畫引擎</h1>
 * <p>
 * 本引擎專為 JavaFX 設計，具備以下核心技術特性：
 * <ul>
 *   <li><b>非線性自研物理緩動 (Cubic Bezier & Overshoot)</b>：模擬空氣阻力與彈簧呼吸手感。</li>
 *   <li><b>動態可中斷狀態機 (Interruptible Animation State Machine)</b>：滑鼠快速劃過中斷時，捕獲當前瞬態值 (Transient Value) 無縫接續反向過渡，徹底告別跳格閃爍。</li>
 *   <li><b>GPU 硬體加速與渲染防掉幀 (Transform-Only & Bitmapped Cache)</b>：僅操作 Translate/Scale/Opacity，嚴禁 Layout Pass，轉場時自動調用 CacheHint.SPEED。</li>
 *   <li><b>零侵入式架構 (Non-intrusive Wrapper)</b>：透過 EventFilter/Handler 掛載，100% 隔離核心業務邏輯。</li>
 * </ul>
 *
 * @author ZeroLauncher & PCL2 Animation Lab
 */
public final class PclAnimationEngine {

    private PclAnimationEngine() {
    }

    // =========================================================================
    // 1. 自研三次貝茲曲線物理插值器 (Custom Cubic Bezier Interpolator)
    // =========================================================================

    /**
     * 高精度三次貝茲曲線插值器（支援自訂控制點與 Overshoot 彈力回彈）
     */
    public static class PclCubicBezier extends Interpolator {
        private static final double EPSILON = 1e-5;
        private static final int MAX_ITERATIONS = 12;

        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        public PclCubicBezier(double x1, double y1, double x2, double y2) {
            this.x1 = clamp(x1, 0.0, 1.0);
            this.y1 = y1;
            this.x2 = clamp(x2, 0.0, 1.0);
            this.y2 = y2;
        }

        private static double clamp(double val, double min, double max) {
            return Math.max(min, Math.min(max, val));
        }

        private double sampleCurveX(double t) {
            // ((1 - 3*x2 + 3*x1)*t + (3*x2 - 6*x1))*t + 3*x1
            return ((1.0 - 3.0 * x2 + 3.0 * x1) * t + (3.0 * x2 - 6.0 * x1)) * t * t + 3.0 * x1 * t;
        }

        private double sampleCurveY(double t) {
            return ((1.0 - 3.0 * y2 + 3.0 * y1) * t + (3.0 * y2 - 6.0 * y1)) * t * t + 3.0 * y1 * t;
        }

        private double sampleCurveDerivativeX(double t) {
            return (3.0 * (1.0 - 3.0 * x2 + 3.0 * x1) * t + 2.0 * (3.0 * x2 - 6.0 * x1)) * t + 3.0 * x1;
        }

        private double solveCurveX(double x) {
            if (x <= 0.0) return 0.0;
            if (x >= 1.0) return 1.0;

            // 1. 牛頓-拉弗森法 (Newton-Raphson) 快速逼近
            double t = x;
            for (int i = 0; i < 8; i++) {
                double xSample = sampleCurveX(t) - x;
                if (Math.abs(xSample) < EPSILON) {
                    return t;
                }
                double d = sampleCurveDerivativeX(t);
                if (Math.abs(d) < 1e-6) {
                    break;
                }
                t -= xSample / d;
            }

            // 2. 二分法 (Bisection) 兜底保證數值穩定
            double t0 = 0.0;
            double t1 = 1.0;
            t = x;

            for (int i = 0; i < MAX_ITERATIONS; i++) {
                double xSample = sampleCurveX(t);
                if (Math.abs(xSample - x) < EPSILON) {
                    return t;
                }
                if (x > xSample) {
                    t0 = t;
                } else {
                    t1 = t;
                }
                t = (t1 + t0) * 0.5;
            }

            return t;
        }

        @Override
        protected double curve(double progress) {
            if (progress <= 0.0) return 0.0;
            if (progress >= 1.0) return 1.0;
            return sampleCurveY(solveCurveX(progress));
        }

        @Override
        public String toString() {
            return String.format("PclCubicBezier(%.3f, %.3f, %.3f, %.3f)", x1, y1, x2, y2);
        }
    }

    // =========================================================================
    // 2. PCL2 經典物理預設曲線 (PCL2 Physics Presets)
    // =========================================================================

    /**
     * PCL2 靈動彈簧曲線：初速極快，在接近終點時伴隨空氣阻力般強烈減速 (0.08, 0.85, 0.18, 1.0)
     */
    public static final Interpolator PCL_FLUID_SPRING = new PclCubicBezier(0.08, 0.85, 0.18, 1.0);

    /**
     * PCL2 呼吸回彈曲線 (Overshoot)：超越目標值 102%~104% 後如同橡皮筋般微幅拉回 (0.18, 1.25, 0.22, 1.0)
     */
    public static final Interpolator PCL_OVERSHOOT = new PclCubicBezier(0.18, 1.25, 0.22, 1.0);

    /**
     * PCL2 強力減速曲線：用於視窗或卡片滑出 (0.12, 0.95, 0.22, 1.0)
     */
    public static final Interpolator PCL_DECELERATE = new PclCubicBezier(0.12, 0.95, 0.22, 1.0);

    /**
     * PCL2 乾脆按下反饋曲線 (0.25, 0.1, 0.25, 1.0)
     */
    public static final Interpolator PCL_SNAP_BACK = new PclCubicBezier(0.25, 0.1, 0.25, 1.0);


    // =========================================================================
    // 3. 動態可中斷狀態機 (Interruptible Animation State Machine)
    // =========================================================================

    /**
     * 針對單一節點與特定屬性的動畫狀態控制器
     */
    private static class NodeAnimationState {
        Timeline activeTimeline;
        boolean isHovered = false;
        boolean isPressed = false;
    }

    private static final Map<Node, NodeAnimationState> STATE_MAP = new ConcurrentHashMap<>();

    private static NodeAnimationState getState(Node node) {
        return STATE_MAP.computeIfAbsent(node, k -> new NodeAnimationState());
    }

    /**
     * 可中斷的平滑屬性過渡核心（自動捕獲瞬態值為起點，動態計算剩餘時長）
     *
     * @param node         目標節點
     * @param property     目標屬性（例如 scaleXProperty, opacityProperty 等）
     * @param targetVal    目標數值
     * @param defaultDuration 基礎預設時長
     * @param interpolator 插值器
     * @param onFinished   完成回調（可為 null）
     */
    public static void animatePropertyInterruptible(
            Node node,
            DoubleProperty property,
            double targetVal,
            Duration defaultDuration,
            Interpolator interpolator,
            Runnable onFinished
    ) {
        if (!AnimationUtils.isAnimationEnabled()) {
            property.set(targetVal);
            if (onFinished != null) onFinished.run();
            return;
        }

        NodeAnimationState state = getState(node);

        // 1. 若當前已有動畫正在播放，立即停止並精確捕獲當前瞬態值
        if (state.activeTimeline != null) {
            state.activeTimeline.stop();
        }

        double currentVal = property.get();
        if (Math.abs(currentVal - targetVal) < 1e-5) {
            property.set(targetVal);
            if (onFinished != null) onFinished.run();
            return;
        }

        // 2. 開啟 GPU 硬體加速快取
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);

        // 3. 動態計算動量時長（依據剩餘差距百分比，讓回彈反應更迅捷）
        double progressNeeded = Math.min(1.0, Math.abs(targetVal - currentVal));
        Duration actualDuration = defaultDuration.multiply(Math.max(0.4, progressNeeded));

        // 4. 構建以當前瞬態值為起點的平滑 KeyFrame
        KeyValue kv = new KeyValue(property, targetVal, interpolator);
        KeyFrame kf = new KeyFrame(actualDuration, kv);

        Timeline timeline = new Timeline(kf);
        state.activeTimeline = timeline;

        timeline.setOnFinished(e -> {
            state.activeTimeline = null;
            // 動畫結束，還原快取模式以保證最高清晰度
            node.setCache(false);
            if (onFinished != null) {
                onFinished.run();
            }
        });

        timeline.play();
    }


    // =========================================================================
    // 4. 核心非侵入式裝飾方法 (Non-Intrusive Wrappers)
    // =========================================================================

    /**
     * <b>【PCL2 級別懸浮微縮放與動態回彈效果】</b>
     * <p>
     * 當滑鼠移入時，按鈕在 200ms 內平滑膨脹至目標倍率（如 1.04x）；
     * 當滑鼠中途移出時，即刻自當前膨脹點無縫收縮回 1.0x，完全不掉幀、不跳格。
     *
     * @param target      目標節點（按鈕、卡片、圖標等）
     * @param targetScale 懸浮放大倍率（推薦 1.03 ~ 1.05）
     * @param durationMs  過渡時長（毫秒，推薦 180 ~ 240ms）
     */
    public static void applyPclHoverEffect(Node target, double targetScale, double durationMs) {
        if (target == null) return;

        Duration duration = Duration.millis(durationMs);

        EventHandler<MouseEvent> enterHandler = event -> {
            NodeAnimationState state = getState(target);
            state.isHovered = true;

            // 雙軸硬體加速縮放
            animateScale(target, targetScale, duration, PCL_FLUID_SPRING);
        };

        EventHandler<MouseEvent> exitHandler = event -> {
            NodeAnimationState state = getState(target);
            state.isHovered = false;

            animateScale(target, 1.0, duration, PCL_DECELERATE);
        };

        // 使用 EventFilter 確保不干擾組件原有點擊業務事件
        target.addEventFilter(MouseEvent.MOUSE_ENTERED, enterHandler);
        target.addEventFilter(MouseEvent.MOUSE_EXITED, exitHandler);
    }

    /**
     * <b>【PCL2 級別卡片彈性點擊回饋 (Click Feedback)】</b>
     * <p>
     * 滑鼠按下時微幅內縮 (0.96x)，滑鼠釋放時帶有微幅呼吸彈跳 (Overshoot: 102% -> 100%)。
     *
     * @param target 目標節點
     */
    public static void applyPclPressEffect(Node target) {
        if (target == null) return;

        target.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            animateScale(target, 0.96, Duration.millis(120), PCL_SNAP_BACK);
        });

        target.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            NodeAnimationState state = getState(target);
            double restoreScale = state.isHovered ? 1.03 : 1.0;
            animateScale(target, restoreScale, Duration.millis(240), PCL_OVERSHOOT);
        });
    }

    /**
     * <b>【PCL2 級別全功能互動一鍵封裝】</b>
     * <p>
     * 同步附加「懸浮靈動放大」與「按下彈性回彈」，賦予按鈕宛如實體開關般的極致手感。
     *
     * @param target 目標節點（按鈕、版本卡片、自訂列表項）
     */
    public static void applyPclInteractive(Node target) {
        applyPclHoverEffect(target, 1.035, 200);
        applyPclPressEffect(target);
    }

    /**
     * <b>【PCL2 級別頁面/彈窗噴射式滑入 (Slide-Up & Fade-In)】</b>
     * <p>
     * 採用非線性空氣阻力物理曲線，從 Y 軸位移快速噴射並極速煞車淡入。
     *
     * @param target     進場節點
     * @param offsetY    起始 Y 軸偏移量（例如 18px）
     * @param durationMs 動畫時長（例如 250ms）
     */
    public static void applyPclEntrance(Node target, double offsetY, double durationMs) {
        if (target == null) return;

        if (!AnimationUtils.isAnimationEnabled()) {
            target.setTranslateY(0);
            target.setOpacity(1.0);
            target.setScaleX(1.0);
            target.setScaleY(1.0);
            return;
        }

        target.setCache(true);
        target.setCacheHint(CacheHint.SPEED);

        target.setTranslateY(offsetY);
        target.setOpacity(0.0);
        target.setScaleX(0.975);
        target.setScaleY(0.975);

        Duration duration = Duration.millis(durationMs);

        Timeline timeline = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(target.translateYProperty(), 0.0, PCL_FLUID_SPRING),
                        new KeyValue(target.opacityProperty(), 1.0, PCL_FLUID_SPRING),
                        new KeyValue(target.scaleXProperty(), 1.0, PCL_OVERSHOOT),
                        new KeyValue(target.scaleYProperty(), 1.0, PCL_OVERSHOOT)
                )
        );

        timeline.setOnFinished(e -> {
            target.setCache(false);
        });

        timeline.play();
    }

    // =========================================================================
    // 5. 內部私有輔助方法 (Internal Helpers)
    // =========================================================================

    private static void animateScale(Node node, double targetScale, Duration duration, Interpolator interpolator) {
        animatePropertyInterruptible(node, node.scaleXProperty(), targetScale, duration, interpolator, null);
        animatePropertyInterruptible(node, node.scaleYProperty(), targetScale, duration, interpolator, null);
    }
}
