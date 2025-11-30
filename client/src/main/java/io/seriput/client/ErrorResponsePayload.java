package io.seriput.client;

record ErrorResponsePayload(
    int errorCode,
    String errorMessage
) { }
