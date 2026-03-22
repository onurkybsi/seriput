package io.seriput.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a request to be sent over a {@code SeriputConnection}.
 *
 * @param payload request payload
 * @param onRequestSent callback to invoke after the payload has been fully written
 * @param onCompleted callback to invoke once the response payload has been received
 */
record PendingRequest(ByteBuffer payload, Runnable onRequestSent, CompletableFuture<byte[]> onCompleted) {}
