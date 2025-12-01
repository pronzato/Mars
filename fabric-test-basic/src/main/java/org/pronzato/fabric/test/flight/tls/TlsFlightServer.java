package org.pronzato.fabric.test.flight.tls;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flight.TestFlightProducer;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.nio.file.Path;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** TLS-enabled Flight server using the demo certificates in {@code src/main/resources/tls}. */
public final class TlsFlightServer {

  public static final int PORT = 33342;
  private static final String BIND_HOST = "0.0.0.0";
  private static final String ADVERTISE_HOST = "localhost";
  private static final String CERT_RESOURCE = TestSecurityConfig.TLS_CERT_RESOURCE;
  private static final String KEY_RESOURCE = TestSecurityConfig.TLS_KEY_RESOURCE;

  private TlsFlightServer() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flight-tls-server");
    TestFlightProducer producer =
        new TestFlightProducer(allocator, () -> PORT, ADVERTISE_HOST, true);

    Path cert = FlightDemoUtils.copyResourceToTempFile(CERT_RESOURCE, "flight-cert", ".pem");
    Path key = FlightDemoUtils.copyResourceToTempFile(KEY_RESOURCE, "flight-key", ".pem");

    FlightServer server =
        FlightServer.builder(allocator, Location.forGrpcTls(BIND_HOST, PORT), producer)
            .useTls(cert.toFile(), key.toFile())
            .build()
            .start();

    System.out.println("[TlsFlightServer] Listening on port " + server.getPort());
    FlightDemoUtils.addShutdownHook(server, allocator);
    server.awaitTermination();
  }
}
