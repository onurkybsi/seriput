package io.seriput.client;

import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Represents a response to be processed by the user application. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
final class PendingResponse {
  @Getter private final CompletableFuture<byte[]> onCompleted;

  @Getter @Setter private byte[] payload;

  boolean isReadyToBeCompleted() {
    return this.payload != null;
  }
}
