package com.github.trosenkrantz.raptor;

public interface RaptorService {
    String getPromptValue();

    String getParameterKey();

    String getDescription();

    void configure(Configuration configuration) throws Exception;

    void run(Configuration configuration) throws Exception;
}
