package com.github.trosenkrantz.raptor.tls;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Trust manager that accepts every certificate without verifying chains or hostnames.
 *
 * <p>This extends {@link X509ExtendedTrustManager} so that JSSE does not wrap it in a manager that
 * performs endpoint identification, which would otherwise reject certificates whose SAN does not
 * match the connected hostname.</p>
 */
public class AllTrustingTrustManager extends X509ExtendedTrustManager {
    private static final Logger LOGGER = Logger.getLogger(AllTrustingTrustManager.class.getName());

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {
        logClientCertificate(certs);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {
        logServerCertificate(certs);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType, Socket socket) {
        logClientCertificate(certs);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType, Socket socket) {
        logServerCertificate(certs);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType, SSLEngine engine) {
        logClientCertificate(certs);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType, SSLEngine engine) {
        logServerCertificate(certs);
    }

    private static void logClientCertificate(X509Certificate[] certs) {
        LOGGER.info("Client presented " + TlsUtility.certificateChainToInfoString(List.of(certs)) + ".");
        LOGGER.fine("Detailed certificates information: " + Arrays.toString(certs));
    }

    private static void logServerCertificate(X509Certificate[] certs) {
        LOGGER.info("Server presented " + TlsUtility.certificateChainToInfoString(List.of(certs)) + ".");
        LOGGER.fine("Detailed certificates information: " + Arrays.toString(certs));
    }
}
