package io.seriput.client;

sealed interface Response permits SuccessResponse, ErrorResponse {
  ResponseStatus status();

  Object value();
}
