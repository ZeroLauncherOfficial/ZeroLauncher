/*
 * ZeroLauncher
 * Copyright (C) 2020 Zero <Zero@zerolauncher.net> and contributors
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
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.zero.launcher.ui.decorator.DecoratorAnimatedPage;

public enum ContainerAnimations implements TransitionPane.AnimationProducer {
    NONE {
        @Override
        public void init(TransitionPane container, Node previousNode, Node nextNode) {
            AnimationUtils.reset(previousNode, false);
            AnimationUtils.reset(nextNode, true);
        }

        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            return new Timeline();
        }

        @Override
        public TransitionPane.AnimationProducer opposite() {
            return this;
        }
    },

    /**
     * PCL2 絲滑微縮放淡入轉場 (WPF Smooth Fade)
     */
    FADE {
        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            Interpolator ease = Motion.PCL_HOVER;
            return new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(previousNode.opacityProperty(), 1.0, ease),
                            new KeyValue(previousNode.scaleXProperty(), 1.0, ease),
                            new KeyValue(previousNode.scaleYProperty(), 1.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 0.0, ease),
                            new KeyValue(nextNode.scaleXProperty(), 0.985, ease),
                            new KeyValue(nextNode.scaleYProperty(), 0.985, ease),
                            new KeyValue(nextNode.translateYProperty(), 12.0, ease)),
                    new KeyFrame(duration.multiply(0.4),
                            new KeyValue(previousNode.opacityProperty(), 0.0, ease),
                            new KeyValue(previousNode.scaleXProperty(), 0.99, ease),
                            new KeyValue(previousNode.scaleYProperty(), 0.99, ease)),
                    new KeyFrame(duration,
                            new KeyValue(nextNode.opacityProperty(), 1.0, ease),
                            new KeyValue(nextNode.scaleXProperty(), 1.0, ease),
                            new KeyValue(nextNode.scaleYProperty(), 1.0, ease),
                            new KeyValue(nextNode.translateYProperty(), 0.0, ease)));
        }

        @Override
        public TransitionPane.AnimationProducer opposite() {
            return this;
        }
    },

    /**
     * A swipe effect
     */
    SWIPE_LEFT {
        @Override
        public void init(TransitionPane container, Node previousNode, Node nextNode) {
            AnimationUtils.reset(previousNode, true);
            AnimationUtils.reset(nextNode, true);
            nextNode.setTranslateX(container.getWidth());
            nextNode.setTranslateY(0);
        }

        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            return new Timeline(new KeyFrame(Duration.ZERO,
                    new KeyValue(nextNode.translateXProperty(), container.getWidth(), interpolator),
                    new KeyValue(previousNode.translateXProperty(), 0, interpolator)),
                    new KeyFrame(duration,
                            new KeyValue(nextNode.translateXProperty(), 0, interpolator),
                            new KeyValue(previousNode.translateXProperty(), -container.getWidth(), interpolator)));
        }

        @Override
        public TransitionPane.AnimationProducer opposite() {
            return SWIPE_RIGHT;
        }
    },

    /**
     * A swipe effect
     */
    SWIPE_RIGHT {
        @Override
        public void init(TransitionPane container, Node previousNode, Node nextNode) {
            AnimationUtils.reset(previousNode, true);
            AnimationUtils.reset(nextNode, true);
            nextNode.setTranslateX(-container.getWidth());
            nextNode.setTranslateY(0);
        }

        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            return new Timeline(new KeyFrame(Duration.ZERO,
                    new KeyValue(nextNode.translateXProperty(), -container.getWidth(), interpolator),
                    new KeyValue(previousNode.translateXProperty(), 0, interpolator)),
                    new KeyFrame(duration,
                            new KeyValue(nextNode.translateXProperty(), 0, interpolator),
                            new KeyValue(previousNode.translateXProperty(), container.getWidth(), interpolator)));
        }

        @Override
        public TransitionPane.AnimationProducer opposite() {
            return SWIPE_LEFT;
        }
    },

    /**
     * <b>【PCL2 規範：錯位疊加向前轉場 (Staggered Offset Forward Transition)】</b>
     * <ul>
     *   <li>舊分頁 (Exit)：透明度 1 -> 0，沿 X 軸向左平移 -20px (200ms)</li>
     *   <li>新分頁 (Enter)：透明度 0 -> 1，沿 X 軸從右側 +40px 向左滑入 (260ms)</li>
     *   <li>50ms 時間差錯位重疊，非線性減速貝氏曲線</li>
     * </ul>
     */
    FORWARD {
        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            Interpolator ease = Motion.PCL_HOVER;
            return new Timeline(
                    // 0ms: 起始態
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(previousNode.translateXProperty(), 0.0, ease),
                            new KeyValue(previousNode.opacityProperty(), 1.0, ease),
                            new KeyValue(nextNode.translateXProperty(), 40.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 0.0, ease)),
                    // 120ms: 舊分頁開始淡出，新分頁開始進入（50ms 錯位疊加）
                    new KeyFrame(duration.multiply(0.45),
                            new KeyValue(previousNode.translateXProperty(), -20.0, ease),
                            new KeyValue(previousNode.opacityProperty(), 0.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 0.45, ease)),
                    // 260ms: 新分頁抵達目標位置
                    new KeyFrame(duration,
                            new KeyValue(nextNode.translateXProperty(), 0.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 1.0, ease))
            );
        }

        @Override
        public TransitionPane.AnimationProducer opposite() {
            return BACKWARD;
        }
    },

    /**
     * <b>【PCL2 規範：錯位疊加向後轉場 (Staggered Offset Backward Transition)】</b>
     */
    BACKWARD {
        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            Interpolator ease = Motion.PCL_HOVER;
            return new Timeline(
                    // 0ms: 起始態
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(previousNode.translateXProperty(), 0.0, ease),
                            new KeyValue(previousNode.opacityProperty(), 1.0, ease),
                            new KeyValue(nextNode.translateXProperty(), -40.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 0.0, ease)),
                    // 120ms: 舊分頁開始淡出，新分頁開始進入（50ms 錯位疊加）
                    new KeyFrame(duration.multiply(0.45),
                            new KeyValue(previousNode.translateXProperty(), 20.0, ease),
                            new KeyValue(previousNode.opacityProperty(), 0.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 0.45, ease)),
                    // 260ms: 新分頁抵達目標位置
                    new KeyFrame(duration,
                            new KeyValue(nextNode.translateXProperty(), 0.0, ease),
                            new KeyValue(nextNode.opacityProperty(), 1.0, ease))
            );
        }

        @Override
        public TransitionPane.AnimationProducer opposite() {
            return FORWARD;
        }
    },

    /// Imitates the animation when switching tabs in the Windows 11 Settings interface
    SLIDE_UP_FADE_IN {
        @Override
        public Timeline animate(
                Pane container, Node previousNode, Node nextNode,
                Duration duration, Interpolator interpolator) {
            return new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(previousNode.translateYProperty(), 0, interpolator),
                            new KeyValue(previousNode.opacityProperty(), 1, interpolator),
                            new KeyValue(nextNode.opacityProperty(), 0, interpolator),
                            new KeyValue(nextNode.translateYProperty(), 10, interpolator)),
                    new KeyFrame(duration.multiply(0.4),
                            new KeyValue(previousNode.opacityProperty(), 0, interpolator)),
                    new KeyFrame(duration,
                            new KeyValue(nextNode.opacityProperty(), 1, interpolator),
                            new KeyValue(nextNode.translateYProperty(), 0, interpolator))
            );
        }
    },

    NAVIGATION {
        @Override
        public Animation animate(Pane container, Node previousNode, Node nextNode, Duration duration, Interpolator interpolator) {
            Timeline timeline = new Timeline();
            Duration halfDuration = duration.divide(2);

            timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO,
                    new KeyValue(previousNode.opacityProperty(), 1, interpolator)));
            timeline.getKeyFrames().add(new KeyFrame(halfDuration,
                    new KeyValue(previousNode.opacityProperty(), 0, interpolator)));
            if (previousNode instanceof DecoratorAnimatedPage prevPage) {
                Node left = prevPage.getLeft();
                Node center = prevPage.getCenter();

                timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO,
                        new KeyValue(left.translateXProperty(), 0, interpolator),
                        new KeyValue(center.translateXProperty(), 0, interpolator)));
                timeline.getKeyFrames().add(new KeyFrame(halfDuration,
                        new KeyValue(left.translateXProperty(), -30, interpolator),
                        new KeyValue(center.translateXProperty(), 30, interpolator)));
            }

            timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO,
                    new KeyValue(nextNode.opacityProperty(), 0, interpolator),
                    new KeyValue(nextNode.translateYProperty(), 10, interpolator)));
            timeline.getKeyFrames().add(new KeyFrame(duration,
                    new KeyValue(nextNode.opacityProperty(), 1, interpolator),
                    new KeyValue(nextNode.translateYProperty(), 0, interpolator)));
            if (nextNode instanceof DecoratorAnimatedPage nextPage) {
                Node left = nextPage.getLeft();
                Node center = nextPage.getCenter();

                timeline.getKeyFrames().add(new KeyFrame(halfDuration,
                        new KeyValue(left.translateXProperty(), -30, interpolator),
                        new KeyValue(center.translateXProperty(), 30, interpolator)));
                timeline.getKeyFrames().add(new KeyFrame(duration,
                        new KeyValue(left.translateXProperty(), 0, interpolator),
                        new KeyValue(center.translateXProperty(), 0, interpolator)));
            }

            return timeline;
        }
    },
    ;
}
