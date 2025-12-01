package org.pronzato.fabric.test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared Kerberos and TLS configuration for all smoke tests. Update the constants in this class to
 * point at real paths/principals for your environment.
 */
public final class TestSecurityConfig {

  private TestSecurityConfig() {}

  public static final String REALM = "EXAMPLE.COM";

  public static final Path KEYTAB = Paths.get("C:/security/keytabs/fabric-smoke.keytab");
  public static final Path KRB5_CONF =
      Paths.get("fabric-test-basic/src/main/resources/krb5.conf");

  public static final String TLS_CERT_RESOURCE = "tls/demo-service.pem";
  public static final String TLS_KEY_RESOURCE = "tls/demo-service.key";
  public static final String TLS_TRUST_RESOURCE = "tls/demo-ca.pem";

  private static final String DEFAULT_SERVICE_HOST = resolveServiceHost();

  public static final String AMPS_SERVICE_PRINCIPAL =
      "HTTP/" + DEFAULT_SERVICE_HOST + "@" + REALM;
  public static final String AMPS_CLIENT_PRINCIPAL = "svc_fabric@" + REALM;

  public static final String GRPC_SERVICE_PRINCIPAL =
      "HTTP/" + DEFAULT_SERVICE_HOST + "@" + REALM;
  public static final String GRPC_CLIENT_PRINCIPAL = "svc_grpc@" + REALM;

  public static final String FLIGHT_SERVICE_PRINCIPAL =
      "HTTP/" + DEFAULT_SERVICE_HOST + "@" + REALM;
  public static final String FLIGHT_CLIENT_PRINCIPAL = "svc_flight@" + REALM;

  public static final String FLIGHT_SQL_SERVICE_PRINCIPAL =
      "HTTP/" + DEFAULT_SERVICE_HOST + "@" + REALM;
  public static final String FLIGHT_SQL_CLIENT_PRINCIPAL = "svc_flightsql@" + REALM;

  private static String resolveServiceHost() {
    String prop = System.getProperty("fabric.test.serviceHost");
    if (prop != null && !prop.isBlank()) {
      return prop;
    }
    String env = System.getenv("FABRIC_TEST_SERVICE_HOST");
    if (env != null && !env.isBlank()) {
      return env;
    }
    // Use a clearly fake default to encourage callers to override with a routable host.
    return "smoke.apps.example.com";
  }
}
