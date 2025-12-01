package org.pronzato.fabric.test.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Small helpers shared across the demo entrypoints. */
public final class GrpcDemoUtils {

  private GrpcDemoUtils() {}

  public static Path copyResourceToTempFile(String resourcePath, String prefix, String suffix)
      throws IOException {
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IllegalArgumentException("Missing resource: " + resourcePath);
      }
      Path temp = Files.createTempFile(prefix, suffix);
      Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
      temp.toFile().deleteOnExit();
      return temp;
    }
  }

  public static void blockUntilShutdown(Server server) throws InterruptedException {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    server.shutdownNow();
                    server.awaitTermination(5, TimeUnit.SECONDS);
                  } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                  }
                },
                "grpc-demo-shutdown"));
    server.awaitTermination();
  }

  public static void closeChannel(ManagedChannel channel) {
    if (channel == null) {
      return;
    }
    channel.shutdownNow();
    try {
      channel.awaitTermination(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

