package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

public record Transition(String input, String output, String nextState) {
    public byte[] outputAsBytes() {
        return BytesFormatter.hexEscapedStringToBytes(output);
    }
}
