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
package org.zero.launcher.game;

import org.zero.launcher.util.DigestUtils;
import org.zero.launcher.util.StringUtils;
import org.zero.launcher.util.gson.JsonSerializable;

import java.io.IOException;
import java.nio.file.Path;

/// @author Zero
@JsonSerializable
public record AssetObject(String hash, long size) {
    public AssetObject {
        if (StringUtils.isBlank(hash) || hash.length() < 2) {
            throw new IllegalArgumentException("Invalid asset hash: " + hash);
        }
    }

    public String getLocation() {
        return hash.substring(0, 2) + "/" + hash;
    }

    public boolean validateChecksum(Path file, boolean defaultValue) throws IOException {
        if (hash == null) return defaultValue;
        return DigestUtils.digestToString("SHA-1", file).equalsIgnoreCase(hash);
    }
}
