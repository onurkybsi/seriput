package io.seriput.server;

import io.seriput.server.core.Cache;
import io.seriput.server.serialization.request.DeleteRequest;
import io.seriput.server.serialization.request.GetRequest;
import io.seriput.server.serialization.request.PutRequest;
import io.seriput.server.serialization.request.RequestDeserializer;
import io.seriput.server.serialization.response.ResponseSerializer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class RequestHandlerImpl implements RequestHandler {
  private static final Cache cache = new Cache();

  private final ArrayList<RequestInterceptor> interceptors = new ArrayList<>();

  RequestHandlerImpl(List<RequestInterceptor> interceptors) {
    this.interceptors.addAll(interceptors);
  }

  @Override
  public ByteBuffer handle(byte[] requestPayload) {
    var deserialized = RequestDeserializer.deserialize(requestPayload);
    this.interceptors.forEach(i -> i.before(requestPayload));
    var response = switch (deserialized) {
      case GetRequest getRequest -> get(getRequest);
      case PutRequest putRequest -> put(putRequest);
      case DeleteRequest deleteRequest -> delete(deleteRequest);
    };
    this.interceptors.forEach(i -> i.after(requestPayload, response));
    return response;
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
