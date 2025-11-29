package io.seriput.client;

enum Ops {
  GET(0x01),
  PUT(0x02),
  DELETE(0x03);

  private final byte op;

  Ops(int op) {
    this.op = (byte) op;
  }

  byte op() {
    return op;
  }
}
