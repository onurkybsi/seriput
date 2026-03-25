package io.seriput.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a request to be sent over a {@code SeriputConnection}.
 *
 * @param payload request payload
 * @param onPayloadConsumed callback to invoke once the payload buffer is no longer needed
 * @param onCompleted callback to invoke once the response payload has been received
 */
record PendingRequest(
    ByteBuffer payload, Runnable onPayloadConsumed, CompletableFuture<byte[]> onCompleted) {}
