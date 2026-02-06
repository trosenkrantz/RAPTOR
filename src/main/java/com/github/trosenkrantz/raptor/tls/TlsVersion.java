package com.github.trosenkrantz.raptor.tls;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum TlsVersion implements PromptEnum, ConfigurableEnum {
    NONE("n", "[N]o TLS", null, "none"),
    V1_2("1.2", "TLS [1.2]", "TLSv1.2", "1.2"),
    V1_3("1.3", "TLS [1.3]", "TLSv1.3", "1.3");

    private final String promptValue;
    private final String description;
    private final String javaId;
    private final String configurationId;

    TlsVersion(String promptValue, String description, String javaId, String configurationId) {
        this.promptValue = promptValue;
        this.description = description;
        this.javaId = javaId;
        this.configurationId = configurationId;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * Java specific ID for TLS version, e.g. "TLSv1.2"
     * @return ID of the TLS version
     */
    public String getJavaId() {
        return javaId;
    }

    @Override
    public String getConfigurationId() {
        return configurationId;
    }
}
