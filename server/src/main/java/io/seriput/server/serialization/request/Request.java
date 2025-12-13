package io.seriput.server.serialization.request;

/**
 * Represents the request payload sent by the client.
 */
public sealed interface Request permits GetRequest, PutRequest, DeleteRequest { }
