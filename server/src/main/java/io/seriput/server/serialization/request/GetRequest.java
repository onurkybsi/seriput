package io.seriput.server.serialization.request;

import io.seriput.server.core.Key;

/**
 * Represent the GET request payload.
 *
 * @param key key that its value to return
 */
public record GetRequest(Key key) implements Request { }
