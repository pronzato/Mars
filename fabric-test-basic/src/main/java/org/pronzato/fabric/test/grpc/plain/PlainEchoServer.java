package org.pronzato.fabric.test.grpc.plain;

import org.pronzato.fabric.test.grpc.EchoService;
import org.pronzato.fabric.test.grpc.GrpcDemoUtils;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

/** Plaintext gRPC echo server with zero authentication. */
public final class PlainEchoServer {

  public static final int PORT = 50071;

  private PlainEchoServer() {}

  public static void main(String[] args) throws Exception {
    Server server =
        NettyServerBuilder.forPort(PORT)
            .addService(new EchoService("plain"))
            .build()
            .start();

    System.out.println("[PlainGrpcServer] Listening on port " + PORT);
    GrpcDemoUtils.blockUntilShutdown(server);
  }
}

