package org.pronzato.fabric.test.flight.kerberos;

import org.pronzato.fabric.test.flight.FlightDemoUtils;
import org.pronzato.fabric.test.flight.FlightKerberos;
import org.pronzato.fabric.test.flight.TestFlightProducer;
import org.pronzato.fabric.api.kerberos.KerberosLogin;
import org.pronzato.fabric.api.kerberos.KerberosTicketManager;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;

/** TLS + Kerberos Flight server. */
public final class KerberosFlightServer {

  public static final int PORT = 33343;
  private static final String BIND_HOST = "0.0.0.0";
  private static final String ADVERTISE_HOST = "localhost";

  private static final String SERVICE_PRINCIPAL = TestSecurityConfig.FLIGHT_SERVICE_PRINCIPAL;
  private static final Path SERVICE_KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path DEFAULT_KRB5 =
      TestSecurityConfig.KRB5_CONF;

  private static final String CERT_RESOURCE = TestSecurityConfig.TLS_CERT_RESOURCE;
  private static final String KEY_RESOURCE = TestSecurityConfig.TLS_KEY_RESOURCE;

  private KerberosFlightServer() {}

  public static void main(String[] args) throws Exception {
    if (!Files.exists(SERVICE_KEYTAB)) {
      System.err.println(
          "[KerberosFlightServer] Keytab missing: " + SERVICE_KEYTAB.toAbsolutePath());
      return;
    }

    KerberosLogin.Config kerberosConfig =
        new KerberosLogin.Config()
            .enabled(true)
            .principal(SERVICE_PRINCIPAL)
            .keytab(SERVICE_KEYTAB.toAbsolutePath().toString())
            .autoRefresh(true)
            .refreshEvery(Duration.ofHours(1));
    if (Files.exists(DEFAULT_KRB5)) {
      kerberosConfig.krb5ConfigPath(DEFAULT_KRB5.toAbsolutePath().toString());
    }

    KerberosTicketManager.ensureDefaultServiceLogin(kerberosConfig);

    BufferAllocator allocator = FlightDemoUtils.newAllocator("flight-kerberos-server");
    TestFlightProducer producer =
        new TestFlightProducer(allocator, () -> PORT, ADVERTISE_HOST, true);

    Path cert = FlightDemoUtils.copyResourceToTempFile(CERT_RESOURCE, "flight-krb-cert", ".pem");
    Path key = FlightDemoUtils.copyResourceToTempFile(KEY_RESOURCE, "flight-krb-key", ".pem");

    FlightServer server =
        FlightServer.builder(allocator, Location.forGrpcTls(BIND_HOST, PORT), producer)
            .useTls(cert.toFile(), key.toFile())
            .middleware(FlightKerberos.SERVER_KEY, FlightKerberos.serverMiddleware(true))
            .build()
            .start();

    System.out.println(
        "[KerberosFlightServer] Listening on port "
            + server.getPort()
            + " as principal "
            + SERVICE_PRINCIPAL);
    FlightDemoUtils.addShutdownHook(server, allocator);
    server.awaitTermination();
  }
}
