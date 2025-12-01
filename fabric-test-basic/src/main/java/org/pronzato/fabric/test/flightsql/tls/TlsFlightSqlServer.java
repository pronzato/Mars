package org.pronzato.fabric.test.flightsql.tls;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flightsql.TestFlightSqlProducer;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.nio.file.Path;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** TLS-enabled Arrow Flight SQL server. */
public final class TlsFlightSqlServer {

  public static final int PORT = 33352;
  private static final String BIND_HOST = "0.0.0.0";
  private static final String ADVERTISE_HOST = "localhost";
  private static final String CERT_RESOURCE = TestSecurityConfig.TLS_CERT_RESOURCE;
  private static final String KEY_RESOURCE = TestSecurityConfig.TLS_KEY_RESOURCE;

  private TlsFlightSqlServer() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flightsql-tls-server");
    TestFlightSqlProducer producer =
        new TestFlightSqlProducer(allocator, () -> PORT, ADVERTISE_HOST, true);

    Path cert = FlightDemoUtils.copyResourceToTempFile(CERT_RESOURCE, "flightsql-cert", ".pem");
    Path key = FlightDemoUtils.copyResourceToTempFile(KEY_RESOURCE, "flightsql-key", ".pem");

    FlightServer server =
        FlightServer.builder(allocator, Location.forGrpcTls(BIND_HOST, PORT), producer)
            .useTls(cert.toFile(), key.toFile())
            .build()
            .start();

    System.out.println("[TlsFlightSqlServer] Listening on port " + server.getPort());
    FlightDemoUtils.addShutdownHook(server, allocator);
    server.awaitTermination();
  }
}
