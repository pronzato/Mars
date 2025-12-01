package org.pronzato.fabric.test.flightsql;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import java.util.function.IntSupplier;
import org.apache.arrow.flight.CallStatus;
import org.apache.arrow.flight.Criteria;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightProducer;
import org.apache.arrow.flight.FlightProducer.CallContext;
import org.apache.arrow.flight.FlightProducer.ServerStreamListener;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.sql.NoOpFlightSqlProducer;
import org.apache.arrow.flight.sql.impl.FlightSql;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

/**
 * Minimal Flight SQL producer backed by the static dataset from {@link FlightDemoUtils}.
 */
public final class TestFlightSqlProducer extends NoOpFlightSqlProducer {

  private final BufferAllocator allocator;
  private final IntSupplier portSupplier;
  private final String advertiseHost;
  private final boolean tls;

  public TestFlightSqlProducer(
      BufferAllocator allocator, IntSupplier portSupplier, String advertiseHost, boolean tls) {
    this.allocator = allocator;
    this.portSupplier = portSupplier;
    this.advertiseHost = advertiseHost;
    this.tls = tls;
  }

  private Location location() {
    return tls
        ? Location.forGrpcTls(advertiseHost, portSupplier.getAsInt())
        : Location.forGrpcInsecure(advertiseHost, portSupplier.getAsInt());
  }

  @Override
  public FlightInfo getFlightInfoStatement(
      FlightSql.CommandStatementQuery command, CallContext context, org.apache.arrow.flight.FlightDescriptor descriptor) {
    String query = command.getQuery();
    validateQuery(query);
    return FlightSqlDemoUtils.flightInfo(query, location(), descriptor);
  }

  @Override
  public void getStreamStatement(
      FlightSql.TicketStatementQuery ticket, CallContext callContext, ServerStreamListener listener) {
    String query = ticket.getStatementHandle().toStringUtf8();
    validateQuery(query);
    try (VectorSchemaRoot root = VectorSchemaRoot.create(FlightDemoUtils.schema(), allocator)) {
      listener.start(root);
      FlightDemoUtils.populateSampleData(root);
      listener.putNext();
      listener.completed();
    } catch (Exception ex) {
      listener.error(ex);
    }
  }

  @Override
  public void listFlights(
      CallContext context, Criteria criteria, FlightProducer.StreamListener<FlightInfo> listener) {
    listener.onNext(
        FlightSqlDemoUtils.flightInfo(
            FlightSqlDemoUtils.sampleQuery(), location(), null));
    listener.onCompleted();
  }

  private static void validateQuery(String query) {
    if (!FlightSqlDemoUtils.supportsQuery(query)) {
      throw CallStatus.INVALID_ARGUMENT
          .withDescription("Unsupported query: " + query)
          .toRuntimeException();
    }
  }
}
