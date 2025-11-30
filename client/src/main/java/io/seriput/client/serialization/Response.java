package io.seriput.client.serialization;

public sealed interface Response permits SuccessResponse, ErrorResponse {
  ResponseStatus status();

  Object value();
}
