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

package org.zero.launcher.ui;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.zero.launcher.util.MathUtils;

/// High-performance physics-based smooth scrolling engine.
///
/// Features dynamic delta-time animation loop (adapted for 60Hz/120Hz/144Hz/240Hz displays),
/// momentum accumulation, viscous friction damping, and dynamic GPU cache management.
@NotNullByDefault
final class ScrollUtils {

    /// Direction of scrolling movement.
    public enum ScrollDirection {
        /// Upward scroll direction.
        UP(-1),
        /// Rightward scroll direction.
        RIGHT(-1),
        /// Downward scroll direction.
        DOWN(1),
        /// Leftward scroll direction.
        LEFT(1);

        private final int intDirection;

        ScrollDirection(int intDirection) {
            this.intDirection = intDirection;
        }

        /// Returns the integer direction multiplier (-1 or 1).
        public int intDirection() {
            return intDirection;
        }
    }

    private static final double DEFAULT_SPEED = 1.0;
    private static final double DEFAULT_TRACK_PAD_ADJUSTMENT = 5.0;

    /// Exponential decay base per 60Hz frame (0.87 gives natural smooth inertial glide).
    private static final double FRICTION_PER_FRAME = 0.87;

    /// Velocity cutoff threshold below which scrolling halts.
    private static final double VELOCITY_CUTOFF = 0.8;

    /// Maximum scrolling velocity (in pixels per second) to prevent runaway over-scrolling.
    private static final double MAX_VELOCITY = 7500.0;

    /// Determines if the given scroll event originates from a high-precision trackpad.
    public static boolean isTrackPad(ScrollEvent event, ScrollDirection scrollDirection) {
        return switch (scrollDirection) {
            case UP, DOWN -> Math.abs(event.getDeltaY()) < 10.0;
            case LEFT, RIGHT -> Math.abs(event.getDeltaX()) < 10.0;
        };
    }

    /// Determines the primary scroll direction of the given scroll event.
    public static ScrollDirection determineScrollDirection(ScrollEvent event) {
        double deltaX = event.getDeltaX();
        double deltaY = event.getDeltaY();

        if (deltaY == 0.0) {
            return deltaX < 0 ? ScrollDirection.LEFT : ScrollDirection.RIGHT;
        } else {
            return deltaY < 0 ? ScrollDirection.DOWN : ScrollDirection.UP;
        }
    }

    /// Adds physics-based smooth scrolling to the given scroll pane with default speed.
    public static void addSmoothScrolling(ScrollPane scrollPane) {
        addSmoothScrolling(scrollPane, DEFAULT_SPEED);
    }

    /// Adds physics-based smooth scrolling to the given scroll pane with custom speed.
    public static void addSmoothScrolling(ScrollPane scrollPane, double speed) {
        addSmoothScrolling(scrollPane, speed, DEFAULT_TRACK_PAD_ADJUSTMENT);
    }

    /// Adds physics-based smooth scrolling to the given scroll pane with custom speed and trackpad factor.
    public static void addSmoothScrolling(ScrollPane scrollPane, double speed, double trackPadAdjustment) {
        smoothScroll(scrollPane, speed, trackPadAdjustment);
    }

    /// Adds physics-based smooth scrolling to the given virtual flow with default speed.
    public static void addSmoothScrolling(VirtualFlow<?> virtualFlow) {
        addSmoothScrolling(virtualFlow, DEFAULT_SPEED);
    }

    /// Adds physics-based smooth scrolling to the given virtual flow with custom speed.
    public static void addSmoothScrolling(VirtualFlow<?> virtualFlow, double speed) {
        addSmoothScrolling(virtualFlow, speed, DEFAULT_TRACK_PAD_ADJUSTMENT);
    }

    /// Adds physics-based smooth scrolling to the given virtual flow with custom speed and trackpad factor.
    public static void addSmoothScrolling(VirtualFlow<?> virtualFlow, double speed, double trackPadAdjustment) {
        smoothScroll(virtualFlow, speed, trackPadAdjustment);
    }

    private static void smoothScroll(ScrollPane scrollPane, double speed, double trackPadAdjustment) {
        final class ScrollPanePhysicsScroller extends AnimationTimer {
            private double velocityX = 0.0;
            private double velocityY = 0.0;
            private long lastNanoTime = 0L;
            private boolean isRunning = false;

            @Override
            public void handle(long now) {
                if (lastNanoTime == 0L) {
                    lastNanoTime = now;
                    return;
                }

                double dt = (now - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = now;

                // Clamp delta-time to avoid huge leaps after pauses or frame drops
                if (dt > 0.05) {
                    dt = 0.05;
                } else if (dt <= 0.0) {
                    return;
                }

                // Exponential decay independent of monitor refresh rate
                double frictionFactor = Math.pow(FRICTION_PER_FRAME, dt * 60.0);
                velocityX *= frictionFactor;
                velocityY *= frictionFactor;

                @Nullable Node content = scrollPane.getContent();
                if (content == null) {
                    stopScrolling();
                    return;
                }

                // Vertical scrolling calculation
                if (Math.abs(velocityY) > VELOCITY_CUTOFF) {
                    double contentHeight = content.getLayoutBounds().getHeight();
                    double viewportHeight = scrollPane.getViewportBounds().getHeight();
                    double scrollableHeight = Math.max(1.0, contentHeight - viewportHeight);

                    double deltaV = (velocityY * dt) / scrollableHeight;
                    double currentV = scrollPane.getVvalue();
                    double targetV = MathUtils.clamp(currentV + deltaV, 0.0, 1.0);

                    scrollPane.setVvalue(targetV);

                    // Boundary collision energy dissipation
                    if (targetV <= 0.0 || targetV >= 1.0) {
                        velocityY *= 0.3;
                    }
                } else {
                    velocityY = 0.0;
                }

                // Horizontal scrolling calculation
                if (Math.abs(velocityX) > VELOCITY_CUTOFF) {
                    double contentWidth = content.getLayoutBounds().getWidth();
                    double viewportWidth = scrollPane.getViewportBounds().getWidth();
                    double scrollableWidth = Math.max(1.0, contentWidth - viewportWidth);

                    double deltaH = (velocityX * dt) / scrollableWidth;
                    double currentH = scrollPane.getHvalue();
                    double targetH = MathUtils.clamp(currentH + deltaH, 0.0, 1.0);

                    scrollPane.setHvalue(targetH);

                    if (targetH <= 0.0 || targetH >= 1.0) {
                        velocityX *= 0.3;
                    }
                } else {
                    velocityX = 0.0;
                }

                if (Math.abs(velocityX) <= VELOCITY_CUTOFF && Math.abs(velocityY) <= VELOCITY_CUTOFF) {
                    stopScrolling();
                }
            }

            void addImpulse(double deltaX, double deltaY) {
                // Continuous momentum accumulation
                velocityX = MathUtils.clamp(velocityX + deltaX, -MAX_VELOCITY, MAX_VELOCITY);
                velocityY = MathUtils.clamp(velocityY + deltaY, -MAX_VELOCITY, MAX_VELOCITY);

                if (!isRunning) {
                    isRunning = true;
                    lastNanoTime = 0L;

                    @Nullable Node content = scrollPane.getContent();
                    if (content != null) {
                        content.setCache(true);
                        content.setCacheHint(CacheHint.SPEED);
                    }

                    start();
                }
            }

            void stopScrolling() {
                if (isRunning) {
                    stop();
                    isRunning = false;
                    velocityX = 0.0;
                    velocityY = 0.0;
                    lastNanoTime = 0L;

                    @Nullable Node content = scrollPane.getContent();
                    if (content != null) {
                        content.setCache(false);
                    }
                }
            }
        }

        final ScrollPanePhysicsScroller scroller = new ScrollPanePhysicsScroller();

        final EventHandler<MouseEvent> mousePressHandler = event -> scroller.stopScrolling();
        final EventHandler<ScrollEvent> scrollEventHandler = event -> {
            if (event.getEventType() == ScrollEvent.SCROLL) {
                ScrollDirection direction = determineScrollDirection(event);
                boolean isTrackpad = isTrackPad(event, direction);

                double factor = isTrackpad ? (speed / trackPadAdjustment) : speed;

                double rawDeltaX = event.getDeltaX();
                double rawDeltaY = event.getDeltaY();

                // Mouse wheel step amplification vs trackpad linear mapping
                double impulseMultiplier = isTrackpad ? 35.0 : 48.0;

                double impulseX = -rawDeltaX * factor * impulseMultiplier;
                double impulseY = -rawDeltaY * factor * impulseMultiplier;

                scroller.addImpulse(impulseX, impulseY);
                event.consume();
            }
        };

        if (scrollPane.getContent() != null && scrollPane.getContent().getParent() != null) {
            scrollPane.getContent().getParent().addEventFilter(MouseEvent.MOUSE_PRESSED, mousePressHandler);
            scrollPane.getContent().getParent().addEventHandler(ScrollEvent.ANY, scrollEventHandler);
        }

        scrollPane.getContent().parentProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeEventFilter(MouseEvent.MOUSE_PRESSED, mousePressHandler);
                oldValue.removeEventHandler(ScrollEvent.ANY, scrollEventHandler);
            }
            if (newValue != null) {
                newValue.addEventFilter(MouseEvent.MOUSE_PRESSED, mousePressHandler);
                newValue.addEventHandler(ScrollEvent.ANY, scrollEventHandler);
            }
        });
    }

    private static void smoothScroll(VirtualFlow<?> virtualFlow, double speed, double trackPadAdjustment) {
        if (!virtualFlow.isVertical()) {
            return;
        }

        final class VirtualFlowPhysicsScroller extends AnimationTimer {
            private double velocityY = 0.0;
            private long lastNanoTime = 0L;
            private boolean isRunning = false;

            @Override
            public void handle(long now) {
                if (lastNanoTime == 0L) {
                    lastNanoTime = now;
                    return;
                }

                double dt = (now - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = now;

                if (dt > 0.05) {
                    dt = 0.05;
                } else if (dt <= 0.0) {
                    return;
                }

                double frictionFactor = Math.pow(FRICTION_PER_FRAME, dt * 60.0);
                velocityY *= frictionFactor;

                if (Math.abs(velocityY) > VELOCITY_CUTOFF) {
                    double pixels = velocityY * dt;
                    virtualFlow.scrollPixels(pixels);
                } else {
                    stopScrolling();
                }
            }

            void addImpulse(double deltaY) {
                velocityY = MathUtils.clamp(velocityY + deltaY, -MAX_VELOCITY, MAX_VELOCITY);

                if (!isRunning) {
                    isRunning = true;
                    lastNanoTime = 0L;
                    start();
                }
            }

            void stopScrolling() {
                if (isRunning) {
                    stop();
                    isRunning = false;
                    velocityY = 0.0;
                    lastNanoTime = 0L;
                }
            }
        }

        final VirtualFlowPhysicsScroller scroller = new VirtualFlowPhysicsScroller();

        final EventHandler<MouseEvent> mousePressHandler = event -> scroller.stopScrolling();
        final EventHandler<ScrollEvent> scrollEventHandler = event -> {
            if (event.getEventType() == ScrollEvent.SCROLL) {
                ScrollDirection direction = determineScrollDirection(event);
                if (direction == ScrollDirection.LEFT || direction == ScrollDirection.RIGHT) {
                    return;
                }

                boolean isTrackpad = isTrackPad(event, direction);
                double factor = isTrackpad ? (speed / trackPadAdjustment) : speed;
                double rawDeltaY = event.getDeltaY();

                double impulseMultiplier = isTrackpad ? 35.0 : 48.0;
                double impulseY = -rawDeltaY * factor * impulseMultiplier;

                scroller.addImpulse(impulseY);
                event.consume();
            }
        };

        virtualFlow.addEventFilter(MouseEvent.MOUSE_PRESSED, mousePressHandler);
        virtualFlow.addEventFilter(ScrollEvent.ANY, scrollEventHandler);
    }

    private ScrollUtils() {
    }
}
