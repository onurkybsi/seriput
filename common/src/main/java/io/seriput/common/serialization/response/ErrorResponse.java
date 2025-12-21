package io.seriput.common.serialization.response;

/**
 * Represents an error response from the Seriput server.
 *
 * @param errorPayload error value
 */
public record ErrorResponse(ResponseStatus responseStatus,
                            ErrorResponsePayload errorPayload) implements Response<ErrorResponsePayload> {
  @Override
  public ResponseStatus status() {
    return responseStatus;
  }

  @Override
  public ErrorResponsePayload value() {
    return errorPayload;
  }
}
