package org.majun.backend.service;

import java.math.BigDecimal;

public interface OctoPrintService {

    default void verifyConnection(String baseUrl) {
        verifyConnection(baseUrl, null, null);
    }

    void verifyConnection(String baseUrl, String authHeaderKey, String authHeaderValue);

    default void uploadAndStartPrint(String baseUrl, String gcodeFileName, String gcodeAbsolutePath) {
        uploadAndStartPrint(baseUrl, gcodeFileName, gcodeAbsolutePath, null, null);
    }

    void uploadAndStartPrint(String baseUrl, String gcodeFileName, String gcodeAbsolutePath, String authHeaderKey, String authHeaderValue);

    default OctoPrintStatus fetchStatus(String baseUrl) {
        return fetchStatus(baseUrl, null, null);
    }

    OctoPrintStatus fetchStatus(String baseUrl, String authHeaderKey, String authHeaderValue);

    default void cancelCurrent(String baseUrl) {
        cancelCurrent(baseUrl, null, null);
    }

    void cancelCurrent(String baseUrl, String authHeaderKey, String authHeaderValue);

    record OctoPrintStatus(
            String state,
            BigDecimal progress,
            Integer estimatedSecondsLeft,
            BigDecimal toolActual,
            BigDecimal toolTarget,
            BigDecimal bedActual,
            BigDecimal bedTarget
    ) {
    }
}
