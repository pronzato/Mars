package org.pronzato.fabric.test.flight.kerberos;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flight.FlightKerberos;
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
import org.apache.arrow.memory.BufferAllocator;

/** Kerberos-authenticated Flight client running over TLS. */
public final class KerberosFlightClient {

  private static final String TARGET_PRINCIPAL = TestSecurityConfig.FLIGHT_SERVICE_PRINCIPAL;
  private static final String CLIENT_PRINCIPAL = TestSecurityConfig.FLIGHT_CLIENT_PRINCIPAL;
  private static final Path CLIENT_KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path DEFAULT_KRB5 = TestSecurityConfig.KRB5_CONF;
  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private KerberosFlightClient() {}

  public static void main(String[] args) throws Exception {
    if (!Files.exists(CLIENT_KEYTAB)) {
      System.err.println(
          "[KerberosFlightClient] Keytab missing: " + CLIENT_KEYTAB.toAbsolutePath());
      return;
    }

    KerberosLogin.Config kerberosConfig =
        new KerberosLogin.Config()
            .enabled(true)
            .principal(CLIENT_PRINCIPAL)
            .keytab(CLIENT_KEYTAB.toAbsolutePath().toString())
            .autoRefresh(true)
            .refreshEvery(Duration.ofHours(1));
    if (Files.exists(DEFAULT_KRB5)) {
      kerberosConfig.krb5ConfigPath(DEFAULT_KRB5.toAbsolutePath().toString());
    }

    KerberosTicketManager.ensureDefaultClientLogin(kerberosConfig);

    BufferAllocator allocator = FlightDemoUtils.newAllocator("flight-kerberos-client");
    try (InputStream trust = FlightDemoUtils.openResource(TRUST_RESOURCE);
        FlightClient client =
            FlightClient.builder()
                .allocator(allocator)
                .location(Location.forGrpcTls("localhost", KerberosFlightServer.PORT))
                .intercept(FlightKerberos.clientMiddleware(TARGET_PRINCIPAL))
                .useTls()
                .trustedCertificates(trust)
                .overrideHostname("localhost")
                .verifyServer(true)
                .build()) {
      FlightInfo info = client.getInfo(FlightDemoUtils.descriptor());
      for (FlightEndpoint endpoint : info.getEndpoints()) {
        try (FlightStream stream = client.getStream(endpoint.getTicket())) {
          System.out.println("[KerberosFlightClient] endpoint " + endpoint.getTicket());
          FlightDemoUtils.printStream(stream);
        }
      }
    } finally {
      FlightDemoUtils.closeAllocator(allocator);
    }
  }

}
