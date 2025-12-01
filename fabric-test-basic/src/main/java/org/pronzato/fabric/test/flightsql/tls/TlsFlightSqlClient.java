package org.pronzato.fabric.test.flightsql.tls;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flightsql.FlightSqlDemoUtils;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.io.InputStream;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.sql.FlightSqlClient;
import org.apache.arrow.memory.BufferAllocator;

/** TLS client for the Flight SQL server using the demo CA. */
public final class TlsFlightSqlClient {

  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private TlsFlightSqlClient() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flightsql-tls-client");
    try (InputStream trust = FlightDemoUtils.openResource(TRUST_RESOURCE);
        FlightClient baseClient =
            FlightClient.builder()
                .allocator(allocator)
                .location(Location.forGrpcTls("localhost", TlsFlightSqlServer.PORT))
                .useTls()
                .trustedCertificates(trust)
                .overrideHostname("localhost")
                .verifyServer(true)
                .build();
        FlightSqlClient sqlClient = new FlightSqlClient(baseClient)) {
      FlightInfo info = sqlClient.execute(FlightSqlDemoUtils.sampleQuery());
      for (FlightEndpoint endpoint : info.getEndpoints()) {
        try (FlightStream stream = baseClient.getStream(endpoint.getTicket())) {
          System.out.println("[TlsFlightSqlClient] endpoint " + endpoint.getTicket());
          FlightDemoUtils.printStream(stream);
        }
      }
    } finally {
      FlightDemoUtils.closeAllocator(allocator);
    }
  }
}
