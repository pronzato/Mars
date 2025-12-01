package org.pronzato.fabric.test.grpc.tls;

import org.pronzato.fabric.test.TestSecurityConfig;
import org.pronzato.fabric.test.grpc.GrpcDemoUtils;
import org.pronzato.fabric.test.grpc.proto.EchoGrpc;
import org.pronzato.fabric.test.grpc.proto.EchoReply;
import org.pronzato.fabric.test.grpc.proto.EchoRequest;
import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Simple TLS echo client that trusts the bundled demo CA. */
public final class TlsEchoClient {

  private static final String HOST = "localhost";
  private static final String TRUST_RESOURCE = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private TlsEchoClient() {}

  public static void main(String[] args) throws Exception {
    Path trustCert = GrpcDemoUtils.copyResourceToTempFile(TRUST_RESOURCE, "grpc-trust", ".pem");
    SslContext sslContext =
        GrpcSslContexts.forClient().trustManager(trustCert.toFile()).build();

    ManagedChannel channel =
        NettyChannelBuilder.forAddress(HOST, TlsEchoServer.PORT)
            .sslContext(sslContext)
            .overrideAuthority("localhost")
            .build();

    try {
      EchoReply reply =
          EchoGrpc.newBlockingStub(channel)
              .withDeadlineAfter(3, TimeUnit.SECONDS)
              .say(
                  EchoRequest.newBuilder()
                      .setMsg("hello-tls-grpc")
                      .setUsername("tls-client")
                      .build());
      System.out.printf(
          "[TlsGrpcClient] response msg=%s instance=%s group=%s%n",
          reply.getMsg(), reply.getInstance(), reply.getGroup());
    } finally {
      GrpcDemoUtils.closeChannel(channel);
    }
  }
}
