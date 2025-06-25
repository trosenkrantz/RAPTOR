package com.github.trosenkrantz.raptor.gateway.network.impairment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MessageGenerator {
    public static List<byte[]> generateMessages(int messageCount, int messageLength) {
        Random random = new Random(0L);

        List<byte[]> inputs = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            byte[] input = new byte[messageLength];
            random.nextBytes(input);
            inputs.add(input);
        }
        return inputs;
    }
}
