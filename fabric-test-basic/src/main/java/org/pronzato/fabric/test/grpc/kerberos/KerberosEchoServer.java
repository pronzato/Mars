package org.pronzato.fabric.test.grpc.kerberos;

import org.pronzato.fabric.api.grpc.GrpcKerberos;
import org.pronzato.fabric.api.kerberos.KerberosLogin;
import org.pronzato.fabric.api.kerberos.KerberosTicketManager;
import org.pronzato.fabric.test.TestSecurityConfig;
import org.pronzato.fabric.test.grpc.EchoService;
import org.pronzato.fabric.test.grpc.GrpcDemoUtils;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** TLS + Kerberos echo server. Requires a valid host keytab for the configured principal. */
public final class KerberosEchoServer {

  public static final int PORT = 50073;

  private static final String SERVICE_PRINCIPAL = TestSecurityConfig.GRPC_SERVICE_PRINCIPAL;
  private static final Path SERVICE_KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path DEFAULT_KRB5 =
      TestSecurityConfig.KRB5_CONF;

  private static final String CERT_RESOURCE = TestSecurityConfig.TLS_CERT_RESOURCE;
  private static final String KEY_RESOURCE = TestSecurityConfig.TLS_KEY_RESOURCE;
  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private KerberosEchoServer() {}

  public static void main(String[] args) throws Exception {
    if (!Files.exists(SERVICE_KEYTAB)) {
      System.err.println(
          "[KerberosEchoServer] Keytab missing: " + SERVICE_KEYTAB.toAbsolutePath());
      return;
    }

    KerberosLogin.Config kerberosConfig =
        new KerberosLogin.Config()
            .enabled(true)
            .principal(SERVICE_PRINCIPAL)
            .keytab(SERVICE_KEYTAB.toAbsolutePath().toString())
            .autoRefresh(false);
    if (Files.exists(DEFAULT_KRB5)) {
      kerberosConfig.krb5ConfigPath(DEFAULT_KRB5.toAbsolutePath().toString());
    }

    KerberosTicketManager.ensureLogin(
        "grpc-echo-server", kerberosConfig, false, Duration.ofHours(1));

    Path cert = GrpcDemoUtils.copyResourceToTempFile(CERT_RESOURCE, "grpc-krb-cert", ".pem");
    Path key = GrpcDemoUtils.copyResourceToTempFile(KEY_RESOURCE, "grpc-krb-key", ".key");
    Path trust = GrpcDemoUtils.copyResourceToTempFile(TRUST_RESOURCE, "grpc-krb-trust", ".pem");

    SslContext sslContext =
        GrpcSslContexts.forServer(cert.toFile(), key.toFile())
            .trustManager(trust.toFile())
            .sslProvider(SslProvider.OPENSSL)
            .build();

    Server server =
        NettyServerBuilder.forPort(PORT)
            .sslContext(sslContext)
            .intercept(GrpcKerberos.serverAuthInterceptor(true))
            .addService(new EchoService("kerberos"))
            .build()
            .start();

    System.out.println(
        "[KerberosGrpcServer] Listening on port "
            + PORT
            + " as principal "
            + SERVICE_PRINCIPAL);

    GrpcDemoUtils.blockUntilShutdown(server);
  }
}
