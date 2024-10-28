package com.github.trosenkrantz.raptor.auto.reply;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateMachine {
    private final StateMachineConfiguration configuration;
    private final Consumer<byte[]> onOutput;

    private String currentState;
    private StringBuilder buffer = new StringBuilder();

    public StateMachine(final StateMachineConfiguration configuration, final Consumer<byte[]> onOutput) {
        this.configuration = configuration;
        currentState = configuration.startState();
        this.onOutput = onOutput;
    }

    public void onInput(byte[] input) {
        buffer.append(new String(input, StandardCharsets.ISO_8859_1)); // Use ISO 8859-1 to be able to match on arbitrary bytes

        List<Transition> transitions = configuration.states().get(currentState);
        if (transitions == null) throw new IllegalStateException("Auto-reply state " + currentState + " not defined.");

        for (Transition transition : transitions) {
            Matcher matcher = Pattern.compile(transition.input(), Pattern.DOTALL).matcher(buffer); // We expect arbitrary bytes, so we use dotall mode to treat line terminators bytes as any other bytes

            if (matcher.matches()) {
                onOutput.accept(hexStringToBytes(transition.output()));
                if (transition.nextState() != null) {
                    currentState = transition.nextState();
                }
                resetInputBuffer();
                break;
            }
        }
    }

    public void resetInputBuffer() {
        buffer = new StringBuilder();
    }

    private static byte[] hexStringToBytes(String hexString) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        int length = hexString.length();
        for (int i = 0; i < length; i++) {
            char c = hexString.charAt(i);

            if (c == '\\' && i + 3 < length && hexString.charAt(i + 1) == 'x' && isHex(hexString.charAt(i + 2)) && isHex(hexString.charAt(i + 3))) { // Match \xhh
                byteStream.write(Integer.parseInt(hexString.substring(i + 2, i + 4), 16)); // Parse as hex to byte
                i += 3; // Skip past hex
            } else { // Regular character
                byteStream.write((byte) c);
            }
        }

        return byteStream.toByteArray();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
}
