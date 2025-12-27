package io.seriput.server;

import java.nio.ByteBuffer;

/**
 * Interceptor for request processing in the Seriput server.
 * <p>
 * Note that interceptors <b>MUST NOT</b> modify the request or response payloads!
 * That's why the interface is package-private and not intended for public use.
 */
interface RequestInterceptor {
  /**
   * Intercepts the given request payload before processing.
   *
   * @param requestPayload raw bytes of the request payload
   */
  void before(byte[] requestPayload);

  /**
   * Intercepts the given request payload after processing.
   *
   * @param requestPayload raw bytes of the request payload
   * @param responsePayload raw bytes of the response payload
   */
  void after(byte[] requestPayload, ByteBuffer responsePayload);
}
