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
package org.zero.launcher.upgrade;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.zero.launcher.util.DigestUtils;
import org.zero.launcher.util.Lang;
import org.zero.launcher.util.io.IOUtils;
import org.zero.launcher.util.io.JarUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.zero.launcher.util.logging.Logger.LOG;

/// Checks the cryptographic integrity and authenticity of ZeroLauncher.
@NotNullByDefault
public final class IntegrityChecker {
    private IntegrityChecker() {}

    /// System property to disable self integrity verification in development environments.
    public static final boolean DISABLE_SELF_INTEGRITY_CHECK = "true".equals(System.getProperty("zero.self_integrity_check.disable"));

    private static final String SIGNATURE_FILE = "META-INF/zero_signature";

    /// Hardcoded official RSA 4096-bit public key in X.509 DER format (Base64-encoded)
    /// to prevent spoofing via replaced JAR resources.
    private static final String OFFICIAL_PUBLIC_KEY_BASE64 =
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAypkXnIxC3KzQsICAK9lEan3icR8OUREbW1vqANzi7ne0EXAy" +
            "Fh1Ow10HIY+SIWHVhsIkS/mvNyUyW5TIPc22djWNQKf6OukOY2/htJpolj4l/uhPGe4BoFiV8m2jvyKq0X7truazc9/B" +
            "DrLKkoIVIkXb0PenVs1AlFLnXzLEu9Lj4kxfkRO4zdV4CMHYWLWLLP9Dj0/Xg+tj9n/QX/IWVVrBporKiJgg99WztKw7" +
            "wQ/Zd+Syu8vfa6IjpgRojtOiCb8MUvEUHcs8RG/PPRACJ8bNwCaNc0RNLoyfVGjjZGFh95XPr6huJYmn/11j45GTB9I5" +
            "w77JnF83AWwAPyx15z6yHKm4epzXJ9yFrTK5nATQGD1H2i8rdg4ZvsjFE4nxEYTWDTkk9nQdZs3n5Os+pUh8hyyjsa4H" +
            "h2yjmfcivcE/KzyP8JH15OWH9QOHwdfRTDu3v4AxSZCuQKlhaSpYbHFahE+jqP+Pr0+Bc/e5ybu6SdAELlOJrVYU6pkg" +
            "rvYJmkV6Ahfm3P8OZKt72MAGxDfdYNUA70QWMMC1YP+GQYedC+oLA4/BhQ1Su9AfUUdQGN0a8a6uaZFX5QyiA/6KrRUC" +
            "06dQdXi9ZOCx8LY28Snl7TjgmOef15HnnmmQTbARJuR0eenprzOXJXJzkS6/Vx1ak/9gNRA8yMiVM9lIilsCAwEAAQ==";

    private static final byte[] OFFICIAL_PUBLIC_KEY_BYTES = Base64.getDecoder().decode(OFFICIAL_PUBLIC_KEY_BASE64);

    private static PublicKey getPublicKey() throws IOException {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(OFFICIAL_PUBLIC_KEY_BYTES));
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to load public key", e);
        }
    }

    /// Verifies that the given JAR file contains a valid signature signed with the official key.
    static void verifyJar(Path jarPath) throws IOException {
        PublicKey publickey = getPublicKey();
        MessageDigest md = DigestUtils.getDigest("SHA-512");

        byte[] signature = null;
        Map<String, byte[]> fileFingerprints = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            for (ZipEntry entry : Lang.toIterable(zip.entries())) {
                String filename = entry.getName();
                try (InputStream in = zip.getInputStream(entry)) {
                    if (in == null) {
                        throw new IOException("entry is null");
                    }

                    if (SIGNATURE_FILE.equals(filename)) {
                        signature = IOUtils.readFully(in);
                    } else {
                        md.reset();
                        fileFingerprints.put(filename, DigestUtils.digest(md, in));
                    }
                }
            }
        }

        if (signature == null) {
            throw new IOException("Signature is missing");
        }

        try {
            Signature verifier = Signature.getInstance("SHA512withRSA");
            verifier.initVerify(publickey);
            for (Entry<String, byte[]> entry : fileFingerprints.entrySet()) {
                md.reset();
                verifier.update(md.digest(entry.getKey().getBytes(UTF_8)));
                verifier.update(entry.getValue());
            }
            if (!verifier.verify(signature)) {
                throw new IOException("Invalid signature: " + jarPath);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to verify signature", e);
        }
    }

    private static volatile @Nullable Boolean selfVerified = null;

    /// Checks whether the current application JAR is verified.
    /// This method is blocking.
    public static boolean isSelfVerified() {
        if (selfVerified != null) {
            return selfVerified;
        }

        synchronized (IntegrityChecker.class) {
            if (selfVerified != null) {
                return selfVerified;
            }

            try {
                Path jarPath = JarUtils.thisJarPath();
                if (jarPath == null) {
                    throw new IOException("Failed to find current ZeroLauncher location");
                }

                verifyJar(jarPath);
                LOG.info("Successfully verified current JAR");
                selfVerified = true;
            } catch (IOException e) {
                LOG.warning("Failed to verify myself, is the JAR corrupt?", e);
                selfVerified = false;
            }

            return selfVerified;
        }
    }

    /// Checks whether the current running instance is official and verified.
    public static boolean isOfficial() {
        return !DISABLE_SELF_INTEGRITY_CHECK && isSelfVerified();
    }
}
