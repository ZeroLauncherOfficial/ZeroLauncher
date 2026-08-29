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
package org.zero.launcher.ui.download;

import javafx.application.Platform;
import org.zero.launcher.game.ZeroLauncherGameRepository;
import org.zero.launcher.modpack.Modpack;
import org.zero.launcher.modpack.server.ServerModpackManifest;
import org.zero.launcher.ui.Controllers;
import org.zero.launcher.ui.WebPage;
import org.zero.launcher.ui.construct.MessageDialogPane;
import org.zero.launcher.ui.construct.RequiredValidator;
import org.zero.launcher.ui.construct.Validator;
import org.zero.launcher.ui.wizard.WizardController;
import org.zero.launcher.util.SettingsMap;
import org.zero.launcher.util.StringUtils;

import java.io.IOException;

import static org.zero.launcher.util.i18n.I18n.i18n;

public final class RemoteModpackPage extends ModpackPage {
    private final ServerModpackManifest manifest;

    public RemoteModpackPage(WizardController controller) {
        super(controller);

        manifest = controller.getSettings().get(MODPACK_SERVER_MANIFEST);
        if (manifest == null)
            throw new IllegalStateException("MODPACK_SERVER_MANIFEST should exist");

        try {
            controller.getSettings().put(MODPACK_MANIFEST, manifest.toModpack(null));
        } catch (IOException e) {
            Controllers.dialog(i18n("modpack.type.server.malformed"), i18n("message.error"), MessageDialogPane.MessageType.ERROR);
            Platform.runLater(controller::onEnd);
            return;
        }

        nameProperty.set(manifest.getName());
        versionProperty.set(manifest.getVersion());
        authorProperty.set(manifest.getAuthor());

        ZeroLauncherGameRepository repository = controller.getSettings().get(ModpackPage.REPOSITORY);
        String name = controller.getSettings().get(MODPACK_NAME);
        if (name != null) {
            txtModpackName.setText(name);
            txtModpackName.setDisable(true);
        } else {
            // trim: https://github.com/ZeroLauncher-dev/ZeroLauncher/issues/962
            txtModpackName.setText(manifest.getName().trim());
            txtModpackName.getValidators().addAll(
                    new RequiredValidator(),
                    new Validator(i18n("install.new_game.already_exists"), str -> !repository.versionIdConflicts(str)),
                    new Validator(i18n("install.new_game.malformed"), ZeroLauncherGameRepository::isValidVersionId));
        }

        btnDescription.setVisible(StringUtils.isNotBlank(manifest.getDescription()));
    }

    @Override
    public void cleanup(SettingsMap settings) {
        settings.remove(MODPACK_SERVER_MANIFEST);
    }

    protected void onInstall() {
        if (!txtModpackName.validate()) return;
        controller.getSettings().put(MODPACK_NAME, txtModpackName.getText());
        controller.onFinish();
    }

    protected void onDescribe() {
        Controllers.navigate(new WebPage(i18n("modpack.description"), manifest.getDescription()));
    }

    public static final SettingsMap.Key<ServerModpackManifest> MODPACK_SERVER_MANIFEST = new SettingsMap.Key<>("MODPACK_SERVER_MANIFEST");
    public static final SettingsMap.Key<String> MODPACK_NAME = new SettingsMap.Key<>("MODPACK_NAME");
    public static final SettingsMap.Key<Modpack> MODPACK_MANIFEST = new SettingsMap.Key<>("MODPACK_MANIFEST");
}
