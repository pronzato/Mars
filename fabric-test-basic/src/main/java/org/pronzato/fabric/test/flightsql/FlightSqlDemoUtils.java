package org.pronzato.fabric.test.flightsql;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.Ticket;
import org.apache.arrow.flight.sql.impl.FlightSql;

/**
 * Shared helpers for the Flight SQL demo entrypoints.
 */
public final class FlightSqlDemoUtils {

  private FlightSqlDemoUtils() {}

  public static final String TABLE_NAME = "demo_records";
  private static final String SAMPLE_QUERY =
      "SELECT id, message, status FROM " + TABLE_NAME;

  public static String normalizedQuery(String query) {
    return query == null
        ? ""
        : query.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
  }

  public static boolean supportsQuery(String query) {
    return normalizedQuery(query).equals(normalizedQuery(SAMPLE_QUERY));
  }

  public static String sampleQuery() {
    return SAMPLE_QUERY;
  }

  public static FlightDescriptor descriptorForQuery(String query) {
    FlightSql.CommandStatementQuery command =
        FlightSql.CommandStatementQuery.newBuilder().setQuery(query).build();
    return FlightDescriptor.command(Any.pack(command).toByteArray());
  }

  public static FlightSql.TicketStatementQuery ticketForQuery(String query) {
    return FlightSql.TicketStatementQuery.newBuilder()
        .setStatementHandle(ByteString.copyFromUtf8(query))
        .build();
  }

  public static String queryFromTicket(Ticket ticket) {
    if (ticket == null) {
      return "";
    }
    try {
      FlightSql.TicketStatementQuery parsed =
          FlightSql.TicketStatementQuery.parseFrom(ticket.getBytes());
      return parsed.getStatementHandle().toStringUtf8();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Invalid ticket payload", e);
    }
  }

  public static FlightEndpoint endpointForQuery(String query, Location location) {
    Ticket ticket = new Ticket(ticketForQuery(query).toByteArray());
    return new FlightEndpoint(ticket, location);
  }

  public static FlightInfo flightInfo(String query, Location location, FlightDescriptor descriptor) {
    FlightDescriptor desc =
        descriptor != null ? descriptor : descriptorForQuery(query);
    FlightEndpoint endpoint = endpointForQuery(query, location);
    long rows = FlightDemoUtils.sampleRowCount();
    return new FlightInfo(
        FlightDemoUtils.schema(),
        desc,
        java.util.List.of(endpoint),
        rows,
        rows);
  }
}
