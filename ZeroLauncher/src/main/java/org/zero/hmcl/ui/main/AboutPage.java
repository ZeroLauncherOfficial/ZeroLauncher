/*
 * ZeroLauncher
 * Copyright (C) 2022  Zero <Zero@zerolauncher.net> and contributors
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
package org.zero.hmcl.ui.main;

import com.google.gson.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import org.zero.hmcl.Metadata;
import org.zero.hmcl.theme.Themes;
import org.zero.hmcl.ui.FXUtils;
import org.zero.hmcl.ui.SVG;
import org.zero.hmcl.ui.WeakListenerHolder;
import org.zero.hmcl.ui.construct.ComponentList;
import org.zero.hmcl.ui.construct.ImageContainer;
import org.zero.hmcl.ui.construct.LineButton;
import org.zero.hmcl.ui.construct.SpinnerPane;
import org.zero.hmcl.util.gson.JsonUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.zero.hmcl.util.i18n.I18n.i18n;
import static org.zero.hmcl.util.logging.Logger.LOG;

public final class AboutPage extends SpinnerPane {

    private final WeakListenerHolder holder = new WeakListenerHolder();

    public AboutPage() {
        VBox content = new VBox();
        content.getStyleClass().add("spinner-pane-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        setContent(scrollPane);

        ComponentList about = new ComponentList();
        {
            var launcher = new LineButton();
            launcher.setLargeTitle(true);
            launcher.setLeading(FXUtils.newBuiltinImage("/assets/img/icon.png"));
            launcher.setTitle("ZeroLauncher");
            launcher.setSubtitle("v" + Metadata.VERSION);

            var author = new LineButton();
            author.setLargeTitle(true);
            author.setTitle(i18n("about.authors"));
            author.setSubtitle("Zero, huangyuhui, Glavo & Contributors");

            about.getContent().setAll(launcher, author);
        }

        ComponentList legal = new ComponentList();
        {
            var copyright = new LineButton();
            copyright.setLargeTitle(true);
            copyright.setTitle(i18n("about.copyright"));
            int currentYear = java.time.Year.now().getValue();
            String copyrightYear = (currentYear > 2026) ? "2013-" + currentYear : "2013-2026";
            copyright.setSubtitle(i18n("about.copyright.statement", copyrightYear));

            var license = new LineButton();
            license.setLargeTitle(true);
            license.setTitle(i18n("about.license"));
            license.setSubtitle("GNU General Public License v3.0");
            license.setOnAction(e -> FXUtils.openLink("https://www.gnu.org/licenses/gpl-3.0.html"));

            legal.getContent().setAll(copyright, license);
        }

        ComponentList thanks = loadIconedTwoLineList("/assets/about/thanks.json");
        ComponentList deps = loadIconedTwoLineList("/assets/about/deps.json");

        content.getChildren().setAll(
                ComponentList.createComponentListTitle(i18n("about")),
                about,
                ComponentList.createComponentListTitle(i18n("about.thanks_to")),
                thanks,
                ComponentList.createComponentListTitle(i18n("about.dependency")),
                deps,
                ComponentList.createComponentListTitle(i18n("about.legal")),
                legal
        );
    }

    private static Image loadImage(String url) {
        return url.startsWith("/")
                ? FXUtils.newBuiltinImage(url)
                : new Image(url);
    }

    private ComponentList loadIconedTwoLineList(String path) {
        ComponentList componentList = new ComponentList();

        InputStream input = FXUtils.class.getResourceAsStream(path);
        if (input == null) {
            LOG.warning("Resources not found: " + path);
            return componentList;
        }

        try {
            JsonArray array = JsonUtils.fromJsonFully(input, JsonArray.class);

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();

                var button = new LineButton();
                button.setLargeTitle(true);

                if (obj.get("externalLink") instanceof JsonPrimitive externalLink) {
                    button.setTrailingIcon(SVG.OPEN_IN_NEW);

                    String link = externalLink.getAsString();
                    button.setOnAction(event -> FXUtils.openLink(link));
                }

                if (obj.has("image")) {
                    JsonElement image = obj.get("image");
                    if (image.isJsonPrimitive()) {
                        var imageView = new ImageContainer(32, 32);
                        imageView.setImage(loadImage(image.getAsString()));
                        imageView.setMouseTransparent(true);


                        button.setLeading(imageView);
                    } else if (image.isJsonObject()) {
                        holder.add(FXUtils.onWeakChangeAndOperate(Themes.darkModeProperty(), darkMode -> button.setLeading(darkMode
                                ? loadImage(image.getAsJsonObject().get("dark").getAsString())
                                : loadImage(image.getAsJsonObject().get("light").getAsString())
                        )));
                    }
                }

                if (obj.get("title") instanceof JsonPrimitive title)
                    button.setTitle(title.getAsString());
                else if (obj.get("titleLocalized") instanceof JsonPrimitive titleLocalized)
                    button.setTitle(i18n(titleLocalized.getAsString()));

                if (obj.get("subtitle") instanceof JsonPrimitive subtitle)
                    button.setSubtitle(subtitle.getAsString());
                else if (obj.get("subtitleLocalized") instanceof JsonPrimitive subtitleLocalized)
                    button.setSubtitle(i18n(subtitleLocalized.getAsString()));

                componentList.getContent().add(button);
            }
        } catch (IOException | JsonParseException e) {
            LOG.warning("Failed to load list: " + path, e);
        }

        return componentList;
    }
}
