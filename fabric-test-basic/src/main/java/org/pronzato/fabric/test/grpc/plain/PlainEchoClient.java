package org.pronzato.fabric.test.grpc.plain;

import org.pronzato.fabric.test.grpc.GrpcDemoUtils;
import org.pronzato.fabric.test.grpc.proto.EchoGrpc;
import org.pronzato.fabric.test.grpc.proto.EchoReply;
import org.pronzato.fabric.test.grpc.proto.EchoRequest;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.concurrent.TimeUnit;

/** Plaintext echo client for quick manual tests. */
public final class PlainEchoClient {

  private static final String HOST = "127.0.0.1";

  private PlainEchoClient() {}

  public static void main(String[] args) throws InterruptedException {
    ManagedChannel channel =
        NettyChannelBuilder.forAddress(HOST, PlainEchoServer.PORT)
            .usePlaintext()
            .build();

    try {
      EchoReply reply =
          EchoGrpc.newBlockingStub(channel)
              .withDeadlineAfter(3, TimeUnit.SECONDS)
              .say(
                  EchoRequest.newBuilder()
                      .setMsg("hello-plain-grpc")
                      .setUsername("plain-client")
                      .build());
      System.out.printf(
          "[PlainGrpcClient] response msg=%s instance=%s group=%s%n",
          reply.getMsg(), reply.getInstance(), reply.getGroup());
    } finally {
      GrpcDemoUtils.closeChannel(channel);
    }
  }
}

