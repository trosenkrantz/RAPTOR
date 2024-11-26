package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum SendStrategy implements PromptEnum {
    NONE("n", "Do [n]ot send", new NoneSendStrategy()),
    INTERACTIVE("i", "Prompt over console [i]nteractively", new InteractiveSendStrategy()),
    AUTO_REPLY("a", "Configure an [a]uto-reply", new AutoReplySendStrategy());

    private final String promptValue;
    private final String description;
    private final WebSocketSendStrategy strategy;

    SendStrategy(String promptValue, String description, WebSocketSendStrategy strategy) {
        this.promptValue = promptValue;
        this.description = description;
        this.strategy = strategy;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public WebSocketSendStrategy getStrategy() {
        return strategy;
    }
}
