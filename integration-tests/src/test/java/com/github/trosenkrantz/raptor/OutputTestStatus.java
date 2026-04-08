package com.github.trosenkrantz.raptor;

public enum OutputTestStatus {
    PASSED,
    /**
     * Failed and cannot recover.
     */
    FAILED,
    /**
     * How not succeeded yet, try again til timing out.
     */
    PENDING
}
