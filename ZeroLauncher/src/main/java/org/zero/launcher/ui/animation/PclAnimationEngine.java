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
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// PCL2-style physical animation engine and physics interpolators.
@NotNullByDefault
public final class PclAnimationEngine {

    private PclAnimationEngine() {
    }

    // =========================================================================
    // 1. 自研三次貝茲曲線物理插值器 (Custom Cubic Bezier Interpolator)
    // =========================================================================

    /// High-precision cubic bezier interpolator supporting overshoot elastic physics.
    public static class PclCubicBezier extends Interpolator {
        private static final double EPSILON = 1e-5;
        private static final int MAX_ITERATIONS = 12;

        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        /// Constructs a new cubic bezier interpolator with specified control points.
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

    /// PCL2 ultra smooth hover cubic bezier curve (0.25, 1.0, 0.5, 1.0).
    public static final Interpolator PCL_HOVER = new PclCubicBezier(0.25, 1.0, 0.5, 1.0);

    /// PCL2 physical overshoot back-ease spring curve (0.175, 0.885, 0.32, 1.275).
    public static final Interpolator PCL_SPRING = new PclCubicBezier(0.175, 0.885, 0.32, 1.275);

    /// PCL2 fluid spring easing curve with rapid onset (0.08, 0.85, 0.18, 1.0).
    public static final Interpolator PCL_FLUID_SPRING = new PclCubicBezier(0.08, 0.85, 0.18, 1.0);

    /// PCL2 overshoot elastic curve (0.18, 1.25, 0.22, 1.0).
    public static final Interpolator PCL_OVERSHOOT = new PclCubicBezier(0.18, 1.25, 0.22, 1.0);

    /// PCL2 deceleration curve for smooth slide out (0.12, 0.95, 0.22, 1.0).
    public static final Interpolator PCL_DECELERATE = new PclCubicBezier(0.12, 0.95, 0.22, 1.0);

    /// PCL2 snap-back curve for crisp pressing feedback (0.25, 0.1, 0.25, 1.0).
    public static final Interpolator PCL_SNAP_BACK = new PclCubicBezier(0.25, 0.1, 0.25, 1.0);

    // =========================================================================
    // 3. 動態可中斷狀態機 (Interruptible Animation State Machine)
    // =========================================================================

    /// 針對單一節點與特定屬性的動畫狀態控制器
    private static final class NodeAnimationState {
        final Map<DoubleProperty, Timeline> activeTimelines = new HashMap<>();
        boolean isHovered = false;
        boolean isPressed = false;
    }

    private static final String KEY_ANIM_STATE = "org.zero.launcher.ui.animation.PclAnimationEngine.STATE";

    private static NodeAnimationState getState(Node node) {
        Object existing = node.getProperties().get(KEY_ANIM_STATE);
        if (existing instanceof NodeAnimationState state) {
            return state;
        }
        NodeAnimationState state = new NodeAnimationState();
        node.getProperties().put(KEY_ANIM_STATE, state);
        return state;
    }

    /// 可中斷的平滑屬性過渡核心（自動捕獲瞬態值為起點，動態計算剩餘時長）
    public static void animatePropertyInterruptible(
            Node node,
            DoubleProperty property,
            double targetVal,
            Duration defaultDuration,
            Interpolator interpolator,
            @Nullable Runnable onFinished
    ) {
        if (!AnimationUtils.isAnimationEnabled()) {
            property.set(targetVal);
            if (onFinished != null) onFinished.run();
            return;
        }

        NodeAnimationState state = getState(node);

        // 1. 若該屬性當前已有動畫正在播放，立即停止並精確捕獲當前瞬態值
        Timeline running = state.activeTimelines.remove(property);
        if (running != null) {
            running.stop();
        }

        double currentVal = property.get();
        if (Math.abs(currentVal - targetVal) < 1e-5) {
            property.set(targetVal);
            if (state.activeTimelines.isEmpty()) {
                node.setCache(false);
            }
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
        state.activeTimelines.put(property, timeline);

        timeline.setOnFinished(e -> {
            state.activeTimelines.remove(property);
            if (state.activeTimelines.isEmpty()) {
                // 所有動畫結束，還原快取模式以保證最高清晰度
                node.setCache(false);
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });

        timeline.play();
    }

    // =========================================================================
    // 4. 核心非侵入式裝飾方法 (Non-Intrusive Wrappers)
    // =========================================================================

    /// Applies PCL2 physical button hover and click animation.
    public static void applyPclButton(Node button) {
        if (button == null) return;

        Duration hoverDuration = Duration.millis(180);
        Duration pressDuration = Duration.millis(120);
        Duration releaseDuration = Duration.millis(240);

        button.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            NodeAnimationState state = getState(button);
            state.isHovered = true;
            if (!state.isPressed) {
                animateScale(button, 1.025, hoverDuration, PCL_HOVER);
            }
        });

        button.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            NodeAnimationState state = getState(button);
            state.isHovered = false;
            if (!state.isPressed) {
                animateScale(button, 1.0, hoverDuration, PCL_DECELERATE);
            }
        });

        button.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            NodeAnimationState state = getState(button);
            state.isPressed = true;
            animateScale(button, 0.96, pressDuration, PCL_SNAP_BACK);
        });

        button.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            NodeAnimationState state = getState(button);
            state.isPressed = false;
            double target = state.isHovered ? 1.025 : 1.0;
            animateScale(button, target, releaseDuration, PCL_SPRING);
        });
    }

    /// Applies PCL2 physical card and pane hover/click animation.
    public static void applyPclCard(Node card) {
        if (card == null) return;

        Duration hoverDuration = Duration.millis(200);
        Duration pressDuration = Duration.millis(130);
        Duration releaseDuration = Duration.millis(260);

        card.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            NodeAnimationState state = getState(card);
            state.isHovered = true;
            if (!state.isPressed) {
                animateScale(card, 1.018, hoverDuration, PCL_HOVER);
            }
        });

        card.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            NodeAnimationState state = getState(card);
            state.isHovered = false;
            if (!state.isPressed) {
                animateScale(card, 1.0, hoverDuration, PCL_DECELERATE);
            }
        });

        card.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            NodeAnimationState state = getState(card);
            state.isPressed = true;
            animateScale(card, 0.975, pressDuration, PCL_SNAP_BACK);
        });

        card.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            NodeAnimationState state = getState(card);
            state.isPressed = false;
            double target = state.isHovered ? 1.018 : 1.0;
            animateScale(card, target, releaseDuration, PCL_SPRING);
        });
    }

    /// Applies PCL2 physical list item hover and press animation with subtle horizontal glide.
    public static void applyPclListItem(Node item) {
        if (item == null) return;

        Duration hoverDuration = Duration.millis(180);
        Duration pressDuration = Duration.millis(110);
        Duration releaseDuration = Duration.millis(230);

        item.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            NodeAnimationState state = getState(item);
            state.isHovered = true;
            if (!state.isPressed) {
                animateScale(item, 1.012, hoverDuration, PCL_HOVER);
                animateTranslateX(item, 3.0, hoverDuration, PCL_HOVER);
            }
        });

        item.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            NodeAnimationState state = getState(item);
            state.isHovered = false;
            if (!state.isPressed) {
                animateScale(item, 1.0, hoverDuration, PCL_DECELERATE);
                animateTranslateX(item, 0.0, hoverDuration, PCL_DECELERATE);
            }
        });

        item.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            NodeAnimationState state = getState(item);
            state.isPressed = true;
            animateScale(item, 0.985, pressDuration, PCL_SNAP_BACK);
            animateTranslateX(item, 1.5, pressDuration, PCL_SNAP_BACK);
        });

        item.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            NodeAnimationState state = getState(item);
            state.isPressed = false;
            double targetScale = state.isHovered ? 1.012 : 1.0;
            double targetTranslate = state.isHovered ? 3.0 : 0.0;
            animateScale(item, targetScale, releaseDuration, PCL_SPRING);
            animateTranslateX(item, targetTranslate, releaseDuration, PCL_SPRING);
        });
    }

    /// Applies PCL2 navigation tab bar animation.
    public static void applyPclTab(Node tab) {
        if (tab == null) return;

        Duration hoverDuration = Duration.millis(160);
        Duration pressDuration = Duration.millis(110);
        Duration releaseDuration = Duration.millis(220);

        tab.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            NodeAnimationState state = getState(tab);
            state.isHovered = true;
            if (!state.isPressed) {
                animateScale(tab, 1.03, hoverDuration, PCL_HOVER);
            }
        });

        tab.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            NodeAnimationState state = getState(tab);
            state.isHovered = false;
            if (!state.isPressed) {
                animateScale(tab, 1.0, hoverDuration, PCL_DECELERATE);
            }
        });

        tab.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            NodeAnimationState state = getState(tab);
            state.isPressed = true;
            animateScale(tab, 0.95, pressDuration, PCL_SNAP_BACK);
        });

        tab.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            NodeAnimationState state = getState(tab);
            state.isPressed = false;
            double target = state.isHovered ? 1.03 : 1.0;
            animateScale(tab, target, releaseDuration, PCL_SPRING);
        });
    }

    /// Applies general PCL2 interactive physics to the target node.
    public static void applyPclInteractive(Node target) {
        applyPclButton(target);
    }

    /// Smoothly glides a double property to target value with PCL physics.
    public static void smoothNumber(DoubleProperty property, double targetVal, Duration duration) {
        if (property == null) return;
        if (!AnimationUtils.isAnimationEnabled()) {
            property.set(targetVal);
            return;
        }

        Timeline timeline = new Timeline(
                new KeyFrame(duration, new KeyValue(property, targetVal, PCL_HOVER))
        );
        timeline.play();
    }

    // =========================================================================
    // 5. 內部私有輔助方法 (Internal Helpers)
    // =========================================================================

    private static void animateScale(Node node, double targetScale, Duration duration, Interpolator interpolator) {
        animatePropertyInterruptible(node, node.scaleXProperty(), targetScale, duration, interpolator, null);
        animatePropertyInterruptible(node, node.scaleYProperty(), targetScale, duration, interpolator, null);
    }

    private static void animateTranslateX(Node node, double targetX, Duration duration, Interpolator interpolator) {
        animatePropertyInterruptible(node, node.translateXProperty(), targetX, duration, interpolator, null);
    }
}
