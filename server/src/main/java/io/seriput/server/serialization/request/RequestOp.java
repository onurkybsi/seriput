package io.seriput.server.serialization.request;

/**
 * Represents the operation requested by the client.
 */
enum RequestOp {
  GET(0x01),
  PUT(0x02),
  DELETE(0x03);

  private final byte op;

  RequestOp(int op) {
    this.op = (byte) op;
  }

  static RequestOp fromByte(byte op) {
    for (RequestOp requestOp : values()) {
      if (requestOp.op == op) {
        return requestOp;
      }
    }
    return null;
  }

  byte op() {
    return op;
  }
}
