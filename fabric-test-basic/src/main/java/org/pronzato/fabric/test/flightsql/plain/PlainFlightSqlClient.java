package org.pronzato.fabric.test.flightsql.plain;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flightsql.FlightSqlDemoUtils;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.sql.FlightSqlClient;
import org.apache.arrow.memory.BufferAllocator;

/** Simple Flight SQL client targeting the plain server. */
public final class PlainFlightSqlClient {

  private PlainFlightSqlClient() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flightsql-plain-client");
    try (FlightClient baseClient =
            FlightClient.builder()
                .allocator(allocator)
                .location(Location.forGrpcInsecure("localhost", PlainFlightSqlServer.PORT))
                .build();
        FlightSqlClient sqlClient = new FlightSqlClient(baseClient)) {
      FlightInfo info = sqlClient.execute(FlightSqlDemoUtils.sampleQuery());
      for (FlightEndpoint endpoint : info.getEndpoints()) {
        try (FlightStream stream = baseClient.getStream(endpoint.getTicket())) {
          System.out.println("[PlainFlightSqlClient] endpoint " + endpoint.getTicket());
          FlightDemoUtils.printStream(stream);
        }
      }
    } finally {
      FlightDemoUtils.closeAllocator(allocator);
    }
  }
}
