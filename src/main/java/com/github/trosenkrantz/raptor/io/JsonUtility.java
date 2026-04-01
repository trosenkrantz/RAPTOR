package com.github.trosenkrantz.raptor.io;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
