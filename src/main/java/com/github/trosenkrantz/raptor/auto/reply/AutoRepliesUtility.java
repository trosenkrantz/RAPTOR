package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class AutoRepliesUtility {
    public static final String PARAMETER_REPLIES = "replies";

    public static List<String> getCaptureGroups(Matcher matcher) {
        ArrayList<String> result = new ArrayList<>();

        // Group 0 (entire input) is not counted in groupCount()
        for (int i = 0; i <= matcher.groupCount(); i++) {
            String group = matcher.group(i); // This returns in ISO 8859-1, treating a arbitrary byte as a single character
            byte[] groupBytes = group.getBytes(StandardCharsets.ISO_8859_1); // So we convert it to bytes
            String intermediateEncoding = BytesFormatter.bytesToIntermediateEncoding(groupBytes); // And then to intermediate encoding, e.g., \x00 for byte 0
            result.add(intermediateEncoding);
        }

        return result;
    }
}
