package com.github.trosenkrantz.raptor.tls;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TlsUtility {
    private static final Logger LOGGER = Logger.getLogger(TlsUtility.class.getName());

    private static final String PARAMETER_KEY_STORE = "keyStore";
    private static final String PARAMETER_KEY_STORE_PASSWORD = "keyStorePassword";
    private static final String PARAMETER_KEY_PASSWORD = "keyPassword";

    private TlsUtility() {
    }

    /**
     * Prompts the user to configure TLS.
     * @param configuration The configuration to set the TLS parameters in.
     * @param requireKeyStore If true, the user will be required to provide a key store path. This is needed for server-side TLS.
     */
    public static void configureTls(Configuration configuration, boolean requireKeyStore) {
        TlsVersion tlsVersion = ConsoleIo.askForOptions(TlsVersion.class, TlsVersion.NONE);
        configuration.setEnum(tlsVersion);

        if (tlsVersion != TlsVersion.NONE) {
            Optional<String> keyStorePathOptional = configureKeyStorePath(requireKeyStore);
            keyStorePathOptional.ifPresent(keyStorePath -> {
                configuration.setRaptorEncodedString(PARAMETER_KEY_STORE, keyStorePath);

                String keyStorePassword = ConsoleIo.askForString("Password of key store", pw -> {
                    try {
                        loadKeyStore(keyStorePath, pw); // Validate by trying to load
                        return Optional.empty();
                    } catch (Exception e) {
                        return Optional.of("Failed loading key store with password. " + e.getMessage());
                    }
                });
                configuration.setRaptorEncodedString(PARAMETER_KEY_STORE_PASSWORD, keyStorePassword);

                configuration.setRaptorEncodedString(
                        PARAMETER_KEY_PASSWORD,
                        ConsoleIo.askForString("Password of key", keyStorePassword, pw -> {
                            try {
                                loadKey(keyStorePath, keyStorePassword, pw, false); // Validate by trying to load
                                return Optional.empty();
                            } catch (Exception e) {
                                return Optional.of("Failed loading key with password. " + e.getMessage());
                            }
                        })
                );
            });
        }
    }

    private static Optional<String> configureKeyStorePath(boolean requireKeyStore) {
        if (requireKeyStore) {
            return Optional.of(ConsoleIo.askForFile("Absolute or relate path to key store (PKCS #12 or JKS)", "." + File.separator + "KeyStore.p12"));
        } else {
            return ConsoleIo.askForOptionalFile("Absolute or relate path to key store (PKCS #12 or JKS)", "no key store");
        }
    }

    private static KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(determineKeyStoreType(path));
        try (FileInputStream keyStoreStream = new FileInputStream(path)) {
            keyStore.load(keyStoreStream, password.toCharArray());
        }
        return keyStore;
    }

    private static KeyManagerFactory loadKey(String path, String keyStorePassword, String keyPassword, boolean logCerts) throws Exception {
        KeyStore keyStore = loadKeyStore(path, keyStorePassword);
        if (logCerts) {
            List<Certificate> certificates = Collections.list(keyStore.aliases()).stream().flatMap(alias -> {
                try {
                    return Arrays.stream(keyStore.getCertificateChain(alias));
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
            LOGGER.info("Using " + certificateChainToInfoString(certificates) + ".");
            LOGGER.fine("Detailed certificates information: " + certificates);
        }

        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, keyPassword.toCharArray());
        return factory;
    }

    public static String certificateChainToInfoString(List<Certificate> certificates) {
        return "chain of " + certificates.size() + " certificate" + (certificates.size() == 1 ? "" : "s") + ": [" + certificates.stream().map(certificate -> {
            if (certificate instanceof X509Certificate x509) {
                return "subject '" + x509.getSubjectX500Principal().getName() + "' issuer '" + x509.getIssuerX500Principal().getName() + "'";
            } else {
                return "unknown certificate type " + certificate.getType();
            }
        }).collect(Collectors.joining(", ")) + "]";
    }

    public static SSLContext loadSslContext(Configuration configuration) throws Exception {
        KeyManager[] keyManagers;
        Optional<String> keyStorePathOptional = configuration.getRaptorEncodedString(PARAMETER_KEY_STORE);
        if (keyStorePathOptional.isPresent()) {
            keyManagers = loadKey(
                    configuration.requireRaptorEncodedString(PARAMETER_KEY_STORE),
                    configuration.requireRaptorEncodedString(PARAMETER_KEY_STORE_PASSWORD),
                    configuration.requireRaptorEncodedString(PARAMETER_KEY_PASSWORD),
                    true
            ).getKeyManagers();
        } else {
            keyManagers = null; // Configure to not use TLS key, so making Java skip
        }

        SSLContext sslContext = SSLContext.getInstance(configuration.requireEnum(TlsVersion.class).getJavaId());
        sslContext.init(keyManagers, new TrustManager[]{new AllTrustingTrustManager()}, new SecureRandom());

        return sslContext;
    }

    private static String determineKeyStoreType(String keyStorePath) {
        String keyStorePathLowerCase = keyStorePath.toLowerCase(Locale.ROOT);
        if (keyStorePathLowerCase.endsWith(".p12") || keyStorePathLowerCase.endsWith(".pfx")) {
            return "PKCS12";
        } else {
            return "JKS";
        }
    }
}
