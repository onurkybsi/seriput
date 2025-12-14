package io.seriput.server;

import java.nio.ByteBuffer;

@FunctionalInterface
interface RequestHandler {
  /**
   * Handles the given request payload and returns the response payload.
   *
   * @param requestPayload request payload sent by the Seriput client
   * @return server response
   * @implNote Note that, this method must not throw any exception!
   * It should handle if there was an exception and return {@code INTERNAL_ERROR} response!
   */
  ByteBuffer handle(byte[] requestPayload);
}
