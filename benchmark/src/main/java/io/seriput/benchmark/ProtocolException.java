package io.seriput.benchmark;

import java.io.IOException;

final class ProtocolException extends IOException {
  ProtocolException(String message) {
    super(message);
  }
}
