package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

/**
 * Transition in a state machine.
 * Both input and output is in intermediate encoding (see {@link BytesFormatter}).
 *
 * @param input     input to be matched
 * @param output    output to perform if input is matched
 * @param nextState Name of the state to transition to if input is matched
 */
public record Transition(String input, String output, String nextState) {
    public byte[] outputAsBytes(int commandSubstitutionTimeout) {
        return BytesFormatter.intermediateEncodingToBytes(output, commandSubstitutionTimeout);
    }
}
