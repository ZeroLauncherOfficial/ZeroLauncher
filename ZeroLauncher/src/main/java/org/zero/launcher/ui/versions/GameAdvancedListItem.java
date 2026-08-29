/*
 * ZeroLauncher
 * Copyright (C) 2020  Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.launcher.ui.versions;

import javafx.geometry.Pos;
import org.zero.launcher.event.Event;
import org.zero.launcher.event.EventBus;
import org.zero.launcher.event.RefreshedVersionsEvent;
import org.zero.launcher.game.ZeroLauncherGameRepository;
import org.zero.launcher.setting.GameDirectoryManager;
import org.zero.launcher.setting.VersionIconType;
import org.zero.launcher.ui.FXUtils;
import org.zero.launcher.ui.WeakListenerHolder;
import org.zero.launcher.ui.construct.AdvancedListItem;
import org.zero.launcher.ui.construct.ImageContainer;

import java.util.function.Consumer;

import static org.zero.launcher.util.i18n.I18n.i18n;

public class GameAdvancedListItem extends AdvancedListItem {
    private final ImageContainer imageContainer;
    private final WeakListenerHolder holder = new WeakListenerHolder();
    private ZeroLauncherGameRepository repository;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Consumer<Event> onVersionIconChangedListener;

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Consumer<RefreshedVersionsEvent> onRefreshedVersionsListener;

    public GameAdvancedListItem() {
        this.imageContainer = new ImageContainer(LEFT_GRAPHIC_SIZE);
        imageContainer.setMouseTransparent(true);
        AdvancedListItem.setAlignment(imageContainer, Pos.CENTER);
        setLeftGraphic(imageContainer);

        holder.add(FXUtils.onWeakChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(), it -> this.loadVersion()));
    }

    private void loadVersion() {
        String version = GameDirectoryManager.getSelectedInstance();

        boolean repositoryChanged = GameDirectoryManager.getSelectedRepository() != repository;
        if (repositoryChanged) {
            repository = GameDirectoryManager.getSelectedRepository();
            onVersionIconChangedListener = repository.onVersionIconChanged.registerWeak(event -> {
                FXUtils.runInFX(this::loadVersion);
            });

            if (!repository.isLoaded()) {
                onRefreshedVersionsListener = EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class)
                        .registerWeak(event -> FXUtils.runInFX(this::loadVersion));
                return;
            }
        }
        if (version != null && repository != null && repository.hasVersion(version)) {
            setTitle(i18n("version.manage.manage"));
            setSubtitle(version);
            imageContainer.setImage(repository.getVersionIconImage(version));
        } else {
            setTitle(i18n("version.empty"));
            setSubtitle(i18n("version.empty.add"));
            imageContainer.setImage(VersionIconType.DEFAULT.getIcon());
        }
    }
}
