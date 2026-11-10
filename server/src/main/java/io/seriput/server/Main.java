package io.seriput.server;

import java.io.IOException;
import java.util.Optional;

final class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    int port =
        Optional.ofNullable(System.getenv("SERIPUT_PORT")).map(Integer::parseInt).orElse(8080);
    SeriputServer server = new SeriputServer(port);
    addShutdownHookToClose(server);
    server.start();
    server.awaitShutdown();
  }

  // Note: Shutdown hook won't fire when running via `./gradlew :server:run` because Gradle kills
  // the forked JVM without delivering SIGTERM. Use `installDist` scripts or `java -cp` directly.
  private static void addShutdownHookToClose(SeriputServer server) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    server.close();
                  } catch (Exception e) {
                    System.err.println("Failed to close server: " + e);
                  }
                }));
  }
}
