package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.PromptEnum;

public enum SendStrategy implements PromptEnum {
    NONE("n", "Do [n]ot send"),
    INTERACTIVE("i", "Prompt over console [i]nteractively"),
    AUTO_REPLY("a", "Configure an [a]uto-reply");

    private final String promptValue;
    private final String description;

    SendStrategy(String promptValue, String description) {
        this.promptValue = promptValue;
        this.description = description;
    }

    @Override
    public String getPromptValue() {
        return this.promptValue;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
