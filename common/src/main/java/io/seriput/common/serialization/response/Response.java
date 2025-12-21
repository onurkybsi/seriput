package io.seriput.common.serialization.response;

/**
 * Represents a response from the Seriput server, which can be either a success or an error.
 *
 * @param <T> type of the response value
 */
public interface Response<T> {
  ResponseStatus status();

  T value();
}
