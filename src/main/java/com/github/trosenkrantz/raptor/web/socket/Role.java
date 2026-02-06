package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.configuration.ConfigurableEnum;

public enum Role implements PromptEnum, ConfigurableEnum {
    CLIENT("c", "Client", "client"),
    SERVER("s", "Server", "server");

    private final String promptValue;
    private final String description;
    private final String configurationId;

    Role(String promptValue, String description, String configurationId) {
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
        return configurationId;
    }
}
