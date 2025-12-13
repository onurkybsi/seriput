package io.seriput.server.serialization.request;

import io.seriput.server.core.Key;

/**
 * Represent the DELETE request payload.
 *
 * @param key key to delete
 */
public record DeleteRequest(Key key) implements Request { }
