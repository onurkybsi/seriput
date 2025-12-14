package io.seriput.server;

import io.seriput.server.core.Cache;
import io.seriput.server.serialization.request.DeleteRequest;
import io.seriput.server.serialization.request.GetRequest;
import io.seriput.server.serialization.request.PutRequest;
import io.seriput.server.serialization.request.RequestDeserializer;
import io.seriput.server.serialization.response.ResponseSerializer;

import java.nio.ByteBuffer;

final class RequestHandlerImpl implements RequestHandler {
  private static final Cache cache = new Cache();
  private static final RequestHandlerImpl instance =  new RequestHandlerImpl();

  private RequestHandlerImpl() {}

  public static RequestHandler instance() {
    return instance;
  }

  @Override
  public ByteBuffer handle(byte[] requestPayload) {
    return switch (RequestDeserializer.deserialize(requestPayload)) {
      case GetRequest getRequest -> get(getRequest);
      case PutRequest putRequest -> put(putRequest);
      case DeleteRequest deleteRequest -> delete(deleteRequest);
    };
  }

  private static ByteBuffer get(GetRequest request) {
    var value = cache.get(request.key());
    if (value == null) {
      return ResponseSerializer.notFound();
    } else {
      return ResponseSerializer.ok(value);
    }
  }

  private static ByteBuffer put(PutRequest request) {
    cache.put(request.key(), request.value());
    return ResponseSerializer.ok();
  }

  private static ByteBuffer delete(DeleteRequest request) {
    var value = cache.delete(request.key());
    if (value == null) {
      return ResponseSerializer.notFound();
    } else {
      return ResponseSerializer.ok();
    }
  }
}
