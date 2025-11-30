package io.seriput.client;

record SuccessResponse<T>(T value) implements Response {
  @Override
  public ResponseStatus status() {
    return ResponseStatus.OK;
  }
}
