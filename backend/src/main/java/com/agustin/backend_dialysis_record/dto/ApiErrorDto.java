package com.agustin.backend_dialysis_record.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorDto(
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ApiErrorDto of(int status, String code, String message, Map<String, String> fieldErrors) {
        return new ApiErrorDto(status, code, message, fieldErrors, Instant.now());
    }
}
