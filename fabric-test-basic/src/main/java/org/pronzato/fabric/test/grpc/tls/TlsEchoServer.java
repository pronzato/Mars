package org.pronzato.fabric.test.grpc.tls;

import org.pronzato.fabric.test.TestSecurityConfig;
import org.pronzato.fabric.test.grpc.EchoService;
import org.pronzato.fabric.test.grpc.GrpcDemoUtils;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import java.nio.file.Path;

/** TLS-enabled echo server using the demo certificates bundled with the module. */
public final class TlsEchoServer {

  public static final int PORT = 50072;

  private static final String CERT_RESOURCE = TestSecurityConfig.TLS_CERT_RESOURCE;
  private static final String KEY_RESOURCE = TestSecurityConfig.TLS_KEY_RESOURCE;

  private TlsEchoServer() {}

  public static void main(String[] args) throws Exception {
    Path cert = GrpcDemoUtils.copyResourceToTempFile(CERT_RESOURCE, "grpc-svc-cert", ".pem");
    Path key = GrpcDemoUtils.copyResourceToTempFile(KEY_RESOURCE, "grpc-svc-key", ".key");

    SslContext sslContext = GrpcSslContexts.forServer(cert.toFile(), key.toFile()).build();

    Server server =
        NettyServerBuilder.forPort(PORT)
            .sslContext(sslContext)
            .addService(new EchoService("tls"))
            .build()
            .start();

    System.out.println("[TlsGrpcServer] Listening on port " + PORT);
    GrpcDemoUtils.blockUntilShutdown(server);
  }
}
