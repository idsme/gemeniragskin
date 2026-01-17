package com.geminiragskin.exception;

/**
 * Exception for Gemini API-specific errors.
 * Provides structured error handling for different error scenarios.
 */
public class GeminiApiException extends Exception {

    public enum ErrorType {
        /**
         * Invalid or missing API key
         */
        UNAUTHORIZED("Unauthorized - Invalid or missing API key"),

        /**
         * Store not found or already deleted
         */
        STORE_NOT_FOUND("File Search Store not found - may have been deleted"),

        /**
         * Document not found in store
         */
        DOCUMENT_NOT_FOUND("Document not found in store"),

        /**
         * Storage quota exceeded for current tier
         */
        QUOTA_EXCEEDED("Storage quota exceeded - upgrade tier required"),

        /**
         * File size exceeds limit
         */
        FILE_TOO_LARGE("File exceeds maximum size limit (100 MB)"),

        /**
         * Store initialization failed
         */
        STORE_INIT_FAILED("Failed to initialize File Search Store"),

        /**
         * API rate limit exceeded
         */
        RATE_LIMIT_EXCEEDED("API rate limit exceeded - please retry later"),

        /**
         * Network/connection error
         */
        NETWORK_ERROR("Network error connecting to Gemini API"),

        /**
         * Invalid request format
         */
        INVALID_REQUEST("Invalid request format or parameters"),

        /**
         * Server error from Gemini API
         */
        SERVER_ERROR("Gemini API server error"),

        /**
         * Generic/unknown error
         */
        UNKNOWN("Unknown error occurred");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorType errorType;
    private final int httpStatus;
    private final String details;

    public GeminiApiException(ErrorType errorType, String message) {
        this(errorType, message, 500, null);
    }

    public GeminiApiException(ErrorType errorType, String message, int httpStatus) {
        this(errorType, message, httpStatus, null);
    }

    public GeminiApiException(ErrorType errorType, String message, int httpStatus, String details) {
        super(message);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    public GeminiApiException(ErrorType errorType, String message, Throwable cause) {
        this(errorType, message, 500, null);
        initCause(cause);
    }

    /**
     * Factory method to create appropriate exception from HTTP status code.
     */
    public static GeminiApiException fromHttpStatus(int status, String message) {
        return switch (status) {
            case 401, 403 -> new GeminiApiException(ErrorType.UNAUTHORIZED, message, status);
            case 404 -> new GeminiApiException(ErrorType.STORE_NOT_FOUND, message, status);
            case 413 -> new GeminiApiException(ErrorType.FILE_TOO_LARGE, message, status);
            case 429 -> new GeminiApiException(ErrorType.RATE_LIMIT_EXCEEDED, message, status);
            case 400 -> new GeminiApiException(ErrorType.INVALID_REQUEST, message, status);
            case 500, 502, 503, 504 -> new GeminiApiException(ErrorType.SERVER_ERROR, message, status);
            default -> new GeminiApiException(ErrorType.UNKNOWN, message, status);
        };
    }

    /**
     * Detects error type from exception message.
     */
    public static GeminiApiException fromMessage(String message) {
        if (message == null) {
            return new GeminiApiException(ErrorType.UNKNOWN, "Unknown error");
        }

        String lower = message.toLowerCase();

        if (lower.contains("unauthorized") || lower.contains("unauthenticated") || lower.contains("api key")) {
            return new GeminiApiException(ErrorType.UNAUTHORIZED, message);
        } else if (lower.contains("not found") || lower.contains("404")) {
            return new GeminiApiException(ErrorType.STORE_NOT_FOUND, message);
        } else if (lower.contains("resource_exhausted") || lower.contains("quota") || lower.contains("exceeded")) {
            return new GeminiApiException(ErrorType.QUOTA_EXCEEDED, message);
        } else if (lower.contains("too large") || lower.contains("size")) {
            return new GeminiApiException(ErrorType.FILE_TOO_LARGE, message);
        } else if (lower.contains("rate limit") || lower.contains("429")) {
            return new GeminiApiException(ErrorType.RATE_LIMIT_EXCEEDED, message);
        } else if (lower.contains("network") || lower.contains("connection") || lower.contains("timeout")) {
            return new GeminiApiException(ErrorType.NETWORK_ERROR, message);
        } else if (lower.contains("invalid") || lower.contains("malformed")) {
            return new GeminiApiException(ErrorType.INVALID_REQUEST, message);
        } else if (lower.contains("server") || lower.contains("500") || lower.contains("502") || lower.contains("503")) {
            return new GeminiApiException(ErrorType.SERVER_ERROR, message);
        }

        return new GeminiApiException(ErrorType.UNKNOWN, message);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDetails() {
        return details;
    }

    /**
     * Gets user-friendly error message with recovery suggestion.
     */
    public String getUserFriendlyMessage() {
        return switch (errorType) {
            case UNAUTHORIZED ->
                "Authentication failed. Please check your Gemini API key is configured correctly.";
            case STORE_NOT_FOUND ->
                "File Search Store not found. Please restart the application.";
            case DOCUMENT_NOT_FOUND ->
                "Document not found in store. It may have been deleted.";
            case QUOTA_EXCEEDED ->
                "Storage quota exceeded. Please delete some files or upgrade to a higher storage tier.";
            case FILE_TOO_LARGE ->
                "File size exceeds the 100 MB limit. Please upload a smaller file.";
            case STORE_INIT_FAILED ->
                "Failed to initialize File Search Store. Please restart the application.";
            case RATE_LIMIT_EXCEEDED ->
                "Too many requests. Please wait a moment and try again.";
            case NETWORK_ERROR ->
                "Network error connecting to Gemini API. Please check your internet connection.";
            case INVALID_REQUEST ->
                "Invalid request. Please check your input and try again.";
            case SERVER_ERROR ->
                "Gemini API server error. Please try again later.";
            case UNKNOWN ->
                "An unexpected error occurred. Please try again.";
        };
    }

    @Override
    public String toString() {
        return String.format("GeminiApiException(%s): %s [Status: %d]",
            errorType, getMessage(), httpStatus);
    }
}
