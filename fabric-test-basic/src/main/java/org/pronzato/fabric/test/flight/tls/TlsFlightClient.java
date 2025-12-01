package org.pronzato.fabric.test.flight.tls;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.io.InputStream;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** TLS Flight client that trusts the demo CA cert. */
public final class TlsFlightClient {

  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private TlsFlightClient() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flight-tls-client");
    try (InputStream trust = FlightDemoUtils.openResource(TRUST_RESOURCE);
        FlightClient client =
            FlightClient.builder()
                .allocator(allocator)
                .location(Location.forGrpcTls("localhost", TlsFlightServer.PORT))
                .useTls()
                .trustedCertificates(trust)
                .overrideHostname("localhost")
                .verifyServer(true)
                .build()) {
      FlightInfo info = client.getInfo(FlightDemoUtils.descriptor());
      for (FlightEndpoint endpoint : info.getEndpoints()) {
        try (FlightStream stream = client.getStream(endpoint.getTicket())) {
          System.out.println("[TlsFlightClient] endpoint " + endpoint.getTicket());
          FlightDemoUtils.printStream(stream);
        }
      }
    } finally {
      FlightDemoUtils.closeAllocator(allocator);
    }
  }
}
