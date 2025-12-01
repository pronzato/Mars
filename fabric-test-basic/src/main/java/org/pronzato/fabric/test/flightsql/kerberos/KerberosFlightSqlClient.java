package org.pronzato.fabric.test.flightsql.kerberos;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flight.FlightKerberos;
import org.pronzato.fabric.test.flightsql.FlightSqlDemoUtils;
import org.pronzato.fabric.api.kerberos.KerberosLogin;
import org.pronzato.fabric.api.kerberos.KerberosTicketManager;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightEndpoint;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.sql.FlightSqlClient;
import org.apache.arrow.memory.BufferAllocator;

/** Kerberos-enabled Flight SQL client running over TLS. */
public final class KerberosFlightSqlClient {

  private static final String TARGET_PRINCIPAL = TestSecurityConfig.FLIGHT_SQL_SERVICE_PRINCIPAL;
  private static final String CLIENT_PRINCIPAL = TestSecurityConfig.FLIGHT_SQL_CLIENT_PRINCIPAL;
  private static final Path CLIENT_KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path DEFAULT_KRB5 = TestSecurityConfig.KRB5_CONF;
  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private KerberosFlightSqlClient() {}

  public static void main(String[] args) throws Exception {
    if (!Files.exists(CLIENT_KEYTAB)) {
      System.err.println(
          "[KerberosFlightSqlClient] Keytab missing: " + CLIENT_KEYTAB.toAbsolutePath());
      return;
    }

    KerberosLogin.Config kerberosConfig =
        new KerberosLogin.Config()
            .enabled(true)
            .principal(CLIENT_PRINCIPAL)
            .keytab(CLIENT_KEYTAB.toAbsolutePath().toString())
            .autoRefresh(true);
    if (Files.exists(DEFAULT_KRB5)) {
      kerberosConfig.krb5ConfigPath(DEFAULT_KRB5.toAbsolutePath().toString());
    }

    KerberosTicketManager.ensureLogin(
        "flightsql-kerberos-client", kerberosConfig, true, Duration.ofHours(1));

    BufferAllocator allocator = FlightDemoUtils.newAllocator("flightsql-kerberos-client");
    try (InputStream trust = FlightDemoUtils.openResource(TRUST_RESOURCE);
        FlightClient baseClient =
            FlightClient.builder()
                .allocator(allocator)
                .location(Location.forGrpcTls("localhost", KerberosFlightSqlServer.PORT))
                .intercept(FlightKerberos.clientMiddleware(TARGET_PRINCIPAL))
                .useTls()
                .trustedCertificates(trust)
                .overrideHostname("localhost")
                .verifyServer(true)
                .build();
        FlightSqlClient sqlClient = new FlightSqlClient(baseClient)) {
      FlightInfo info = sqlClient.execute(FlightSqlDemoUtils.sampleQuery());
      for (FlightEndpoint endpoint : info.getEndpoints()) {
        try (FlightStream stream = baseClient.getStream(endpoint.getTicket())) {
          System.out.println("[KerberosFlightSqlClient] endpoint " + endpoint.getTicket());
          FlightDemoUtils.printStream(stream);
        }
      }
    } finally {
      FlightDemoUtils.closeAllocator(allocator);
    }
  }

}
