package io.seriput.client.serialization;

record ErrorResponsePayload(
    int errorCode,
    String errorMessage
) { }
