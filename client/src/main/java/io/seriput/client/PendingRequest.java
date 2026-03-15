package io.seriput.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a request to be sent over a {@code SeriputConnection}.
 *
 * @param payload request payload
 * @param callback future to be completed with the response
 */
record PendingRequest(ByteBuffer payload, CompletableFuture<byte[]> callback) {}
