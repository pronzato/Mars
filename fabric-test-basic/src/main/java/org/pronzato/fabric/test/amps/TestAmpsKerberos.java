package org.pronzato.fabric.test.amps;

import com.crankuptheamps.client.Authenticator;
import com.crankuptheamps.client.ConnectionInfo;
import com.crankuptheamps.client.HAClient;
import com.crankuptheamps.client.ServerChooser;
import com.crankuptheamps.client.exception.AMPSException;
import org.pronzato.fabric.api.kerberos.KerberosApi;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Minimal AMPS TLS + Kerberos smoke test. */
public final class TestAmpsKerberos {

  private static final long RECONNECT_BACKOFF_MS = 3_000L;
  private static final String AMPS_URI = "tcps://amps1.corp:9443/amps/json";
  private static final String CLIENT_NAME = "fabric-test-basic";

  private TestAmpsKerberos() {}

  public static void main(String[] args) throws Exception {
    if (!Files.exists(TestSecurityConfig.KEYTAB)) {
      System.err.println(
          "[Kerberos] Keytab missing: " + TestSecurityConfig.KEYTAB.toAbsolutePath());
      return;
    }

    ConnectionFactory factory =
        new KerberosConnectionFactory(
            AMPS_URI,
            CLIENT_NAME,
            TestSecurityConfig.AMPS_CLIENT_PRINCIPAL,
            TestSecurityConfig.KEYTAB,
            TestSecurityConfig.KRB5_CONF);

    AtomicReference<HAClient> clientRef = new AtomicReference<>(factory.connect());
    AtomicBoolean stop = new AtomicBoolean(false);
    ExecutorService guardian =
        Executors.newSingleThreadExecutor(
            r -> {
              Thread t = new Thread(r, "amps-guardian");
              t.setDaemon(true);
              return t;
            });
    guardian.submit(() -> monitorAndReconnect(factory, clientRef, stop));

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  stop.set(true);
                  closeQuietly(clientRef.get());
                  closeQuietly(factory);
                  guardian.shutdownNow();
                  System.out.println("[Main] Shutdown complete.");
                }));

    Thread.currentThread().join();
  }

  private static void monitorAndReconnect(
      ConnectionFactory factory, AtomicReference<HAClient> clientRef, AtomicBoolean stop) {
    while (!stop.get()) {
      try {
        HAClient client = clientRef.get();
        if (client == null) {
          throw new IllegalStateException("Client not connected");
        }
        client.publish("/fabric/test/basic", "{\"status\":\"ok\"}");
        Thread.sleep(Duration.ofSeconds(5).toMillis());
      } catch (Throwable t) {
        System.err.println(
            "[AMPS] connection issue (" + factory.ampsUri() + "): " + t.getMessage());
        closeQuietly(clientRef.get());
        sleep(RECONNECT_BACKOFF_MS);
        try {
          clientRef.set(factory.connect());
          System.out.println(
              "[AMPS] Reconnected to " + factory.ampsUri() + " as " + factory.clientName());
        } catch (Throwable retryFailure) {
          System.err.println(
              "[AMPS] Reconnect failed ("
                  + factory.ampsUri()
                  + "): "
                  + retryFailure.getMessage());
          sleep(RECONNECT_BACKOFF_MS);
        }
      }
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ignore) {
      Thread.currentThread().interrupt();
    }
  }

  private static void closeQuietly(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ignore) {
      }
    }
  }

  private interface ConnectionFactory extends AutoCloseable {
    HAClient connect();

    String ampsUri();

    String clientName();

    @Override
    default void close() {}
  }

  private static final class KerberosConnectionFactory implements ConnectionFactory {
    private final String ampsUri;
    private final String clientName;
    private final String principal;
    private final java.nio.file.Path keytab;
    private final java.nio.file.Path krb5Conf;
    private KerberosApi kerberos;

    KerberosConnectionFactory(
        String ampsUri,
        String clientName,
        String principal,
        java.nio.file.Path keytab,
        java.nio.file.Path krb5Conf) {
      this.ampsUri = requireNonBlank(ampsUri, "ampsUri");
      this.clientName = requireNonBlank(clientName, "clientName");
      this.principal = requireNonBlank(principal, "principal");
      this.keytab = Objects.requireNonNull(keytab, "keytab");
      this.krb5Conf = Objects.requireNonNull(krb5Conf, "krb5Conf");
    }

    @Override
    public HAClient connect() {
      if (kerberos == null) {
        try {
          KerberosApi.Builder builder =
              KerberosApi.builder().withPrincipal(principal).withKeytabPath(keytab);
          if (Files.exists(krb5Conf)) {
            builder.withKrb5Path(krb5Conf);
          }
          kerberos = builder.build().start();
        } catch (Exception e) {
          throw new IllegalStateException("Kerberos bootstrap failed", e);
        }
      }
      return kerberos.doAs(
          () -> {
            try {
              HAClient client = new HAClient(clientName);
              client.setServerChooser(new StaticServerChooser(ampsUri));
              client.connectAndLogon();
              System.out.println(
                  "[AMPS] Connected to "
                      + ampsUri
                      + " as "
                      + clientName
                      + " (Kerberos principal "
                      + principal
                      + ")");
              return client;
            } catch (Exception e) {
              throw new RuntimeException("Kerberos connection failed: " + e.getMessage(), e);
            }
          });
    }

    @Override
    public String ampsUri() {
      return ampsUri;
    }

    @Override
    public String clientName() {
      return clientName;
    }

    @Override
    public void close() {
      closeQuietly(kerberos);
    }
  }

  private static final class StaticServerChooser implements ServerChooser {
    private final String uri;
    private volatile String lastError = "";

    StaticServerChooser(String uri) {
      this.uri = requireNonBlank(uri, "uri");
    }

    @Override
    public String getCurrentURI() {
      return uri;
    }

    @Override
    public Authenticator getCurrentAuthenticator() {
      return null;
    }

    @Override
    public void reportFailure(Exception exception, ConnectionInfo info) {
      if (exception != null) {
        lastError = exception.toString();
      }
    }

    @Override
    public String getError() {
      return lastError;
    }

    @Override
    public void reportSuccess(ConnectionInfo info) {
      lastError = "";
    }
  }

  private static String requireNonBlank(String value, String field) {
    if (value == null) {
      throw new IllegalArgumentException(field + " is required");
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return trimmed;
  }
}
