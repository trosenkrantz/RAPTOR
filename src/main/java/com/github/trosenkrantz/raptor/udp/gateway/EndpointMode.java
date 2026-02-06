package com.github.trosenkrantz.raptor.udp.gateway;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum EndpointMode implements PromptEnum, ConfigurableEnum {
    MULTICAST("m", "[M]ulticast, bidirectional", "multicast");

    private final String promptValue;
    private final String description;
    private final String configurationId;

    EndpointMode(String promptValue, String description, String configurationId) {
        this.promptValue = promptValue;
        this.description = description;
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

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }
}
