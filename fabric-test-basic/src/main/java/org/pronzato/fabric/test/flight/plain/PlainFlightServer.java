package org.pronzato.fabric.test.flight.plain;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flight.TestFlightProducer;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** Insecure Arrow Flight server broadcasting the demo dataset. */
public final class PlainFlightServer {

  public static final int PORT = 33341;
  private static final String BIND_HOST = "0.0.0.0";
  private static final String ADVERTISE_HOST = "localhost";

  private PlainFlightServer() {}

  public static void main(String[] args) throws Exception {
    BufferAllocator allocator = FlightDemoUtils.newAllocator("flight-plain-server");
    TestFlightProducer producer =
        new TestFlightProducer(allocator, () -> PORT, ADVERTISE_HOST, false);
    FlightServer server =
        FlightServer.builder(allocator, Location.forGrpcInsecure(BIND_HOST, PORT), producer)
            .build()
            .start();
    System.out.println("[PlainFlightServer] Listening on port " + server.getPort());
    FlightDemoUtils.addShutdownHook(server, allocator);
    server.awaitTermination();
  }
}
