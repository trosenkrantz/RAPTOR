package com.github.trosenkrantz.raptor.io;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BytesFormatterPropertyTest {

    @Property
    void bytesToAndFromIntermediateEncodingAreIdentity(@ForAll byte[] input) {
        String encoded = BytesFormatter.bytesToIntermediateEncoding(input);
        byte[] decoded = BytesFormatter.intermediateEncodingToBytes(encoded, 1000);
        assertArrayEquals(input, decoded);
    }

    @Property
    void bytesToAndFromRaptorEncodingAreIdentity(@ForAll byte[] input) {
        String encoded = BytesFormatter.bytesToRaptorEncoding(input);
        byte[] decoded = BytesFormatter.raptorEncodingToBytes(encoded, 1000);
        assertArrayEquals(input, decoded);
    }
}
