package io.seriput.client.serialization;

record SuccessResponse<T>(T value) implements Response {
  @Override
  public ResponseStatus status() {
    return ResponseStatus.OK;
  }
}
