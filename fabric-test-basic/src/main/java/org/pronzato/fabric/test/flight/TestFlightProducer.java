package org.pronzato.fabric.test.flight;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import org.apache.arrow.flight.CallStatus;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightProducer;
import org.apache.arrow.flight.FlightProducer.CallContext;
import org.apache.arrow.flight.FlightProducer.ServerStreamListener;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.NoOpFlightProducer;
import org.apache.arrow.flight.Ticket;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

/**
 * Simple in-memory Flight producer that returns the sample dataset from {@link FlightDemoUtils}.
 */
public final class TestFlightProducer extends NoOpFlightProducer {

  private final BufferAllocator allocator;
  private final IntSupplier portSupplier;
  private final String advertiseHost;
  private final boolean tls;

  public TestFlightProducer(
      BufferAllocator allocator, IntSupplier portSupplier, String advertiseHost, boolean tls) {
    this.allocator = Objects.requireNonNull(allocator, "allocator");
    this.portSupplier = Objects.requireNonNull(portSupplier, "portSupplier");
    this.advertiseHost = Objects.requireNonNull(advertiseHost, "advertiseHost");
    this.tls = tls;
  }

  @Override
  public FlightInfo getFlightInfo(CallContext context, FlightDescriptor descriptor) {
    if (!FlightDemoUtils.descriptor().equals(descriptor)) {
      throw CallStatus.INVALID_ARGUMENT
          .withDescription("Unknown descriptor")
          .toRuntimeException();
    }
    Location location =
        tls
            ? Location.forGrpcTls(advertiseHost, portSupplier.getAsInt())
            : Location.forGrpcInsecure(advertiseHost, portSupplier.getAsInt());
    FlightEndpoint endpoint = new FlightEndpoint(FlightDemoUtils.ticket(), location);
    long rows = FlightDemoUtils.sampleRowCount();
    return new FlightInfo(
        FlightDemoUtils.schema(),
        FlightDemoUtils.descriptor(),
        List.of(endpoint),
        rows,
        rows);
  }

  @Override
  public void getStream(CallContext context, Ticket ticket, ServerStreamListener listener) {
    if (!FlightDemoUtils.matchesTicket(ticket)) {
      throw CallStatus.INVALID_ARGUMENT
          .withDescription("Unknown ticket")
          .toRuntimeException();
    }
    try (VectorSchemaRoot root = VectorSchemaRoot.create(FlightDemoUtils.schema(), allocator)) {
      listener.start(root);
      FlightDemoUtils.populateSampleData(root);
      listener.putNext();
      listener.completed();
    } catch (Exception e) {
      listener.error(e);
    }
  }
}
