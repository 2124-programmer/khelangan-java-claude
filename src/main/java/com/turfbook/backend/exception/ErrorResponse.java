package com.turfbook.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;

    /** Optional machine-readable context (e.g. {"allowed": 6, "current": 6} for a court-limit conflict). */
    private Map<String, Object> details;

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
