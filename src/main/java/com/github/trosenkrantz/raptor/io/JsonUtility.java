package com.github.trosenkrantz.raptor.io;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.regex.Pattern;

public class JsonUtility {
    /**
     * Build a mapper that ignores simple line and block comments.
     *
     * @return the mapper
     */
    public static ObjectMapper buildMapper() {
        return JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
    }
}
