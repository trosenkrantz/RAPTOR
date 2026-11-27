package com.github.trosenkrantz.raptor.tls;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

/** Test helper for creating a TLS context with a certificate whose SAN does not match localhost. */
public final class TlsTestSupport {
    private static final char[] PASSWORD = "password".toCharArray();

    private TlsTestSupport() {
    }

    public static SSLContext sslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = TlsTestSupport.class.getResourceAsStream("/tls/key-store.p12")) {
            if (in == null) throw new IllegalStateException("Missing test key store.");
            keyStore.load(in, PASSWORD);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{new AllTrustingTrustManager()}, new SecureRandom());
        return sslContext;
    }
}
