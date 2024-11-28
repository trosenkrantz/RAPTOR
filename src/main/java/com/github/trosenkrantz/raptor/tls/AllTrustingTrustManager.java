package com.github.trosenkrantz.raptor.tls;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Logger;

public class AllTrustingTrustManager implements X509TrustManager {
    private static final Logger LOGGER = Logger.getLogger(AllTrustingTrustManager.class.getName());

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) {
        LOGGER.info("Client presented " + certs.length + " certificates with authentication type " + authType + ": " + Arrays.toString(certs));
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) {
        LOGGER.info("Server presented " + certs.length + " certificates with authentication type " + authType + ": " + Arrays.toString(certs));
    }
}
