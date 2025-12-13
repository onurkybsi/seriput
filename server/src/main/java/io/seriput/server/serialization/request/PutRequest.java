package io.seriput.server.serialization.request;

import io.seriput.server.core.Key;
import io.seriput.server.core.Value;

/**
 * Represent the PUT request payload.
 *
 * @param key key to put
 * @param value value to put
 */
public record PutRequest(Key key, Value value) implements Request { }
