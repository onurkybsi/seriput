package io.seriput.common.serialization.response;

/**
 * Represents a successful response from the Seriput server.
 *
 * @param <T> type of the response value
 */
public record SuccessResponse<T>(T value) implements Response<T> {
  @Override
  public ResponseStatus status() {
    return ResponseStatus.OK;
  }
}
