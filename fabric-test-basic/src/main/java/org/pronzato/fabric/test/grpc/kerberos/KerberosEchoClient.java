package org.pronzato.fabric.test.grpc.kerberos;

import org.pronzato.fabric.api.grpc.GrpcKerberos;
import org.pronzato.fabric.api.kerberos.KerberosLogin;
import org.pronzato.fabric.api.kerberos.KerberosTicketManager;
import org.pronzato.fabric.test.TestSecurityConfig;
import org.pronzato.fabric.test.grpc.GrpcDemoUtils;
import org.pronzato.fabric.test.grpc.proto.EchoGrpc;
import org.pronzato.fabric.test.grpc.proto.EchoReply;
import org.pronzato.fabric.test.grpc.proto.EchoRequest;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Kerberos-authenticated TLS echo client. */
public final class KerberosEchoClient {

  private static final String HOST = "localhost";

  private static final String TARGET_PRINCIPAL = TestSecurityConfig.GRPC_SERVICE_PRINCIPAL;
  private static final String CLIENT_PRINCIPAL = TestSecurityConfig.GRPC_CLIENT_PRINCIPAL;
  private static final Path CLIENT_KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path DEFAULT_KRB5 = TestSecurityConfig.KRB5_CONF;

  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private KerberosEchoClient() {}

  public static void main(String[] args) throws Exception {
    if (!Files.exists(CLIENT_KEYTAB)) {
      System.err.println(
          "[KerberosEchoClient] Keytab missing: " + CLIENT_KEYTAB.toAbsolutePath());
      return;
    }

    KerberosLogin.Config kerberosConfig =
        new KerberosLogin.Config()
            .enabled(true)
            .principal(CLIENT_PRINCIPAL)
            .keytab(CLIENT_KEYTAB.toAbsolutePath().toString())
            .autoRefresh(false);
    if (Files.exists(DEFAULT_KRB5)) {
      kerberosConfig.krb5ConfigPath(DEFAULT_KRB5.toAbsolutePath().toString());
    }

    KerberosTicketManager.ensureLogin(
        "grpc-echo-client", kerberosConfig, false, Duration.ofHours(1));

    Path trustCert = GrpcDemoUtils.copyResourceToTempFile(TRUST_RESOURCE, "grpc-krb-trust", ".pem");
    SslContext sslContext =
        GrpcSslContexts.forClient().trustManager(trustCert.toFile()).build();

    ManagedChannel channel =
        NettyChannelBuilder.forAddress(HOST, KerberosEchoServer.PORT)
            .sslContext(sslContext)
            .overrideAuthority("localhost")
            .build();

    CallCredentials kerberosCreds = GrpcKerberos.clientCallCredentials(TARGET_PRINCIPAL);

    try {
      EchoReply reply =
          EchoGrpc.newBlockingStub(channel)
              .withDeadlineAfter(5, TimeUnit.SECONDS)
              .withCallCredentials(kerberosCreds)
              .say(
                  EchoRequest.newBuilder()
                      .setMsg("hello-kerberos-grpc")
                      .setUsername("kerberos-client")
                      .build());
      System.out.printf(
          "[KerberosGrpcClient] response msg=%s instance=%s group=%s%n",
          reply.getMsg(), reply.getInstance(), reply.getGroup());
    } finally {
      GrpcDemoUtils.closeChannel(channel);
    }
  }

}
