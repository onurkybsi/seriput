package io.seriput.common.serialization.response;

/**
 * Payload for error responses in the Seriput protocol v1.
 */
public record ErrorResponsePayload(Integer errorCode, String errorMessage) { }
