package com.github.trosenkrantz.raptor.tls;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.RaptorPaths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

class TlsUtilityTest {
    @Test
    void loadsKeyStoreWithoutAssumingFormatFromFileName(@TempDir Path configDir) throws Exception {
        RaptorPaths.setBase(configDir);
        char[] password = "password".toCharArray();
        Path keyStorePath = configDir.resolve("key-store"); // No .p12 or .jks extension

        for (String type : new String[]{"PKCS12", "JKS"}) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(null, password);
            try (OutputStream out = Files.newOutputStream(keyStorePath)) {
                keyStore.store(out, password);
            }

            Configuration configuration = Configuration.empty();
            configuration.setEnum(TlsVersion.V1_3);
            configuration.setRaptorEncodedString("keyStore", keyStorePath.getFileName().toString());
            configuration.setRaptorEncodedString("keyStorePassword", new String(password));
            configuration.setRaptorEncodedString("keyPassword", new String(password));

            Assertions.assertEquals("TLSv1.3", TlsUtility.loadSslContext(configuration).getProtocol());
        }
    }
}
