package org.pronzato.fabric.test.flightsql.plain;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flightsql.TestFlightSqlProducer;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** Insecure Arrow Flight SQL server. */
public final class PlainFlightSqlServer {

  public static final int PORT = 33351;
  private static final String BIND_HOST = "0.0.0.0";
  private static final String ADVERTISE_HOST = "localhost";

  private PlainFlightSqlServer() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flightsql-plain-server");
    TestFlightSqlProducer producer =
        new TestFlightSqlProducer(allocator, () -> PORT, ADVERTISE_HOST, false);
    FlightServer server =
        FlightServer.builder(allocator, Location.forGrpcInsecure(BIND_HOST, PORT), producer)
            .build()
            .start();
    System.out.println("[PlainFlightSqlServer] Listening on port " + server.getPort());
    FlightDemoUtils.addShutdownHook(server, allocator);
    server.awaitTermination();
  }
}
