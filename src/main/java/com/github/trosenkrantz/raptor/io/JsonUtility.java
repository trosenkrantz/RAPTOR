package com.github.trosenkrantz.raptor.io;

import java.util.regex.Pattern;

public class JsonUtility {
    private static final Pattern COMMENTS_PATTERN = Pattern.compile("(//.*?$)|(/\\*.*?\\*/)", Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Remove simple line and block comments.
     * <p>
     * We use Jackson ObjectMapper to parse JSON.
     * It does not support comments, so wrap JSON in this method before parsing.
     *
     * @param value JSON string to process
     * @return result
     */
    public static String removeComments(final String value) {
        return COMMENTS_PATTERN.matcher(value).replaceAll("");
    }
}
