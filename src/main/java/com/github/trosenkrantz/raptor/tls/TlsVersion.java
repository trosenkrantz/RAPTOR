package com.github.trosenkrantz.raptor.tls;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum TlsVersion implements PromptEnum {
    NONE("n", "[N]o TLS", null),
    V1_2("1.2", "TLS 1.2", "TLSv1.2"),
    V1_3("1.3", "TLS 1.3", "TLSv1.3");

    private final String promptValue;
    private final String description;
    private final String id;

    TlsVersion(String promptValue, String description, String id) {
        this.promptValue = promptValue;
        this.description = description;
        this.id = id;
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
    public String getId() {
        return id;
    }
}
