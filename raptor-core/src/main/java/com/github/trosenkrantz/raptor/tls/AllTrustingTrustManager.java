package com.github.trosenkrantz.raptor.tls;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class AllTrustingTrustManager implements X509TrustManager {
    private static final Logger LOGGER = Logger.getLogger(AllTrustingTrustManager.class.getName());

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {
        LOGGER.info("Client presented " + TlsUtility.certificateChainToInfoString(List.of(certs)) + ".");
        LOGGER.fine("Detailed certificates information: " + Arrays.toString(certs));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {
        LOGGER.info("Server presented " + TlsUtility.certificateChainToInfoString(List.of(certs)) + ".");
        LOGGER.fine("Detailed certificates information: " + Arrays.toString(certs));
    }
}
