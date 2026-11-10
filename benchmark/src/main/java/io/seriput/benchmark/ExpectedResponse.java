package io.seriput.benchmark;

record ExpectedResponse(byte status, byte valueType, byte[] payload) { // NOSONAR
  ExpectedResponse {
    payload = payload.clone();
  }

  @Override
  public byte[] payload() {
    return payload.clone();
  }
}
