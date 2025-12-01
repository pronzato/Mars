package org.pronzato.fabric.api.grpc;

import org.pronzato.fabric.api.kerberos.KerberosSpnego;
import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * SPNEGO (Kerberos) integration for gRPC.
 *
 * <p>Client: generates "authorization: Negotiate &lt;token&gt;" (per call). Server: validates the
 * token and exposes the authenticated principal via Context key.
 *
 * <p>Recommended to run over TLS.
 */
public final class GrpcKerberos {

  // gRPC metadata header name (lowercase for HTTP/2)
  private static final Metadata.Key<String> AUTHZ =
      Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  // Context key to expose the authenticated principal
  public static final Context.Key<String> AUTHENTICATED_PRINCIPAL = Context.key("krb.principal");

  private GrpcKerberos() {}

  /**
   * Build per-call credentials that inject a SPNEGO "Negotiate" token for the given service
   * principal name (SPN). Typical SPN patterns: "HTTP/host@REALM" or "grpc/host@REALM" depending on
   * your Kerberos setup.
   */
  public static CallCredentials clientCallCredentials(final String targetSpn) {
    return new CallCredentials() {
      @Override
      public void applyRequestMetadata(
          RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(
            () -> {
              try {
                String token =
                    KerberosSpnego.createInitTokenBase64(
                        Objects.requireNonNull(targetSpn, "targetSpn"));
                Metadata md = new Metadata();
                md.put(AUTHZ, "Negotiate " + token);
                applier.apply(md);
              } catch (Exception e) {
                applier.fail(
                    Status.UNAUTHENTICATED.withDescription("Kerberos init failed").withCause(e));
              }
            });
      }

      @Override
      public void thisUsesUnstableApi() {}
    };
  }

  /**
   * Server interceptor that validates an incoming "Negotiate" token. If kerberosRequired==true and
   * token invalid/missing, returns UNAUTHENTICATED. On success, puts the authenticated client
   * principal into Context (AUTHENTICATED_PRINCIPAL).
   */
  public static ServerInterceptor serverAuthInterceptor(boolean kerberosRequired) {
    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        try {
          String authz = headers.get(AUTHZ);
          if (authz == null || !authz.regionMatches(true, 0, "Negotiate ", 0, 10)) {
            if (kerberosRequired) {
              call.close(
                  Status.UNAUTHENTICATED.withDescription("Missing Kerberos token"), new Metadata());
              return new ServerCall.Listener<>() {};
            }
            return next.startCall(call, headers);
          }

          String b64 = authz.substring("Negotiate ".length()).trim();
          byte[] token = Base64.getDecoder().decode(b64);

          KerberosSpnego.AcceptResult result = KerberosSpnego.acceptToken(token);
          if (!result.established()) {
            call.close(
                Status.UNAUTHENTICATED.withDescription(
                    "Kerberos multi-leg not supported in this sample"),
                new Metadata());
            return new ServerCall.Listener<>() {};
          }

          String principal = result.principal() != null ? result.principal() : "unknown";
          Context ctx = Context.current().withValue(AUTHENTICATED_PRINCIPAL, principal);
          return Contexts.interceptCall(ctx, call, headers, next);
        } catch (Exception e) {
          call.close(
              Status.UNAUTHENTICATED.withDescription("Kerberos validation failed").withCause(e),
              new Metadata());
          return new ServerCall.Listener<>() {};
        }
      }
    };
  }
}

