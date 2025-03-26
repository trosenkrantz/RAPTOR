package com.github.trosenkrantz.raptor;

/**
 * Object that can be run independently by RAPTOR.
 */
public interface RootService extends RaptorService {
    /**
     * Runs the service.
     *
     * @param configuration configuration specifying how to run the service.
     * @throws Exception if the service fails.
     */
    void run(Configuration configuration) throws Exception;
}
