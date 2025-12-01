package org.pronzato.fabric.test.grpc;

import org.pronzato.fabric.api.grpc.GrpcKerberos;
import org.pronzato.fabric.test.grpc.proto.EchoData;
import org.pronzato.fabric.test.grpc.proto.EchoGrpc;
import org.pronzato.fabric.test.grpc.proto.EchoReply;
import org.pronzato.fabric.test.grpc.proto.EchoRequest;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal Echo service implementation shared across all demo scenarios.
 *
 * <p>The reply echoes the payload and annotates it with a scenario tag. When Kerberos headers are
 * present the authenticated principal is surfaced as the group name so callers can verify the
 * handshake succeeded.
 */
public final class EchoService extends EchoGrpc.EchoImplBase {

  private final String scenarioTag;
  private final String instanceId;
  private final AtomicInteger counter = new AtomicInteger();

  public EchoService(String scenarioTag) {
    this.scenarioTag = scenarioTag;
    this.instanceId = scenarioTag + "-srv-" + ProcessHandle.current().pid();
  }

  @Override
  public void say(EchoRequest request, StreamObserver<EchoReply> responseObserver) {
    String caller = request.getUsername().isBlank() ? "anonymous" : request.getUsername();
    String principal = GrpcKerberos.AUTHENTICATED_PRINCIPAL.get();
    String groupTag = principal != null ? principal : scenarioTag;

    EchoReply reply =
        EchoReply.newBuilder()
            .setMsg("echo:" + request.getMsg())
            .setInstance(instanceId + "#" + counter.incrementAndGet())
            .setGroup(groupTag)
            .build();

    responseObserver.onNext(reply);
    responseObserver.onCompleted();

    System.out.printf(
        "[EchoService][%s] handled say() for user=%s principal=%s%n",
        scenarioTag, caller, principal != null ? principal : "(none)");
  }

  @Override
  public void stream(EchoData request, StreamObserver<EchoData> responseObserver) {
    EchoData response =
        EchoData.newBuilder()
            .setValue(
                "stream:"
                    + scenarioTag
                    + ":"
                    + counter.incrementAndGet()
                    + ":"
                    + request.getValue()
                    + "@"
                    + Instant.now())
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}


