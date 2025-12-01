package org.pronzato.fabric.test.flight.plain;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** Simple Flight client for the insecure demo server. */
public final class PlainFlightClient {

  private PlainFlightClient() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flight-plain-client");
    try (FlightClient client =
        FlightClient.builder()
            .allocator(allocator)
            .location(Location.forGrpcInsecure("localhost", PlainFlightServer.PORT))
            .build()) {
      FlightInfo info = client.getInfo(FlightDemoUtils.descriptor());
      for (FlightEndpoint endpoint : info.getEndpoints()) {
        try (FlightStream stream = client.getStream(endpoint.getTicket())) {
          System.out.println("[PlainFlightClient] endpoint " + endpoint.getTicket());
          FlightDemoUtils.printStream(stream);
        }
      }
    } finally {
      FlightDemoUtils.closeAllocator(allocator);
    }
  }
}
