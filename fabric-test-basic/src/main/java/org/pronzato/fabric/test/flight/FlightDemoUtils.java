package org.pronzato.fabric.test.flight;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Ticket;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * Shared helpers for the Flight demo services/clients.
 */
public final class FlightDemoUtils {

  private FlightDemoUtils() {}

  private static final Field FIELD_ID =
      new Field("id", FieldType.nullable(new ArrowType.Int(64, true)), null);
  private static final Field FIELD_MESSAGE =
      new Field("message", FieldType.nullable(new ArrowType.Utf8()), null);
  private static final Field FIELD_STATUS =
      new Field("status", FieldType.nullable(new ArrowType.Utf8()), null);

  private static final Schema SAMPLE_SCHEMA = new Schema(List.of(FIELD_ID, FIELD_MESSAGE, FIELD_STATUS));
  private static final FlightDescriptor SAMPLE_DESCRIPTOR = FlightDescriptor.path("demo-flight", "records");
  private static final Ticket SAMPLE_TICKET = new Ticket("demo-flight-records".getBytes(StandardCharsets.UTF_8));

  private static final long[] SAMPLE_IDS = {101L, 102L, 103L, 104L};
  private static final String[] SAMPLE_MESSAGES = {"alpha", "bravo", "charlie", "delta"};
  private static final String[] SAMPLE_STATUS = {"READY", "RUNNING", "FAILED", "SUCCESS"};

  public static BufferAllocator newAllocator(String name) {
    return new RootAllocator();
  }

  public static void closeAllocator(BufferAllocator allocator) {
    if (allocator != null) {
      allocator.close();
    }
  }

  public static Schema schema() {
    return SAMPLE_SCHEMA;
  }

  public static FlightDescriptor descriptor() {
    return SAMPLE_DESCRIPTOR;
  }

  public static Ticket ticket() {
    return SAMPLE_TICKET;
  }

  public static int sampleRowCount() {
    return SAMPLE_IDS.length;
  }

  public static boolean matchesTicket(Ticket ticket) {
    return ticket != null && Objects.equals(new String(ticket.getBytes(), StandardCharsets.UTF_8),
        new String(SAMPLE_TICKET.getBytes(), StandardCharsets.UTF_8));
  }

  public static void populateSampleData(VectorSchemaRoot root) {
    root.allocateNew();
    BigIntVector idVector = (BigIntVector) root.getVector("id");
    VarCharVector messageVector = (VarCharVector) root.getVector("message");
    VarCharVector statusVector = (VarCharVector) root.getVector("status");

    for (int i = 0; i < SAMPLE_IDS.length; i++) {
      idVector.setSafe(i, SAMPLE_IDS[i]);
      messageVector.setSafe(i, SAMPLE_MESSAGES[i].getBytes(StandardCharsets.UTF_8));
      statusVector.setSafe(i, SAMPLE_STATUS[i].getBytes(StandardCharsets.UTF_8));
    }

    root.setRowCount(SAMPLE_IDS.length);
    idVector.setValueCount(SAMPLE_IDS.length);
    messageVector.setValueCount(SAMPLE_IDS.length);
    statusVector.setValueCount(SAMPLE_IDS.length);
  }

  public static void printStream(FlightStream stream) {
    while (stream.next()) {
      VectorSchemaRoot root = stream.getRoot();
      System.out.println(root.contentToTSVString());
    }
  }

  public static void addShutdownHook(FlightServer server, BufferAllocator allocator) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    if (server != null) {
                      server.close();
                    }
                  } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                  } finally {
                    closeAllocator(allocator);
                  }
                },
                "flight-demo-shutdown"));
  }

  public static Path copyResourceToTempFile(String resourcePath, String prefix, String suffix)
      throws IOException {
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IllegalArgumentException("Missing resource: " + resourcePath);
      }
      Path temp = Files.createTempFile(prefix, suffix);
      Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
      temp.toFile().deleteOnExit();
      return temp;
    }
  }

  public static InputStream openResource(String resourcePath) {
    InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    if (in == null) {
      throw new IllegalArgumentException("Missing resource: " + resourcePath);
    }
    return in;
  }
}
