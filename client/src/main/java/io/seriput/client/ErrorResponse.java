package io.seriput.client;

record ErrorResponse(
  ResponseStatus responseStatus,
  ErrorResponsePayload errorPayload
) implements Response {
  @Override
  public ResponseStatus status() {
    return responseStatus;
  }

  @Override
  public Object value() {
    return errorPayload;
  }
}
