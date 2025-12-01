package org.pronzato.fabric.test.flight;

import org.pronzato.fabric.api.kerberos.KerberosSpnego;
import java.util.Base64;
import java.util.Objects;
import org.apache.arrow.flight.CallHeaders;
import org.apache.arrow.flight.CallStatus;
import org.apache.arrow.flight.FlightClientMiddleware;
import org.apache.arrow.flight.FlightClientMiddleware.Factory;
import org.apache.arrow.flight.FlightServerMiddleware;
import org.apache.arrow.flight.RequestContext;

/**
 * Minimal Kerberos helper for Arrow Flight using the same "Negotiate" metadata header as gRPC.
 */
public final class FlightKerberos {

  private FlightKerberos() {}

  private static final String AUTH_HEADER = "authorization";
  private static final String PRINCIPAL_HEADER = "x-kerberos-principal";
  public static final FlightServerMiddleware.Key<KerberosServerMiddleware> SERVER_KEY =
      FlightServerMiddleware.Key.of("flight-kerberos");

  public static FlightServerMiddleware.Factory<KerberosServerMiddleware> serverMiddleware(
      boolean kerberosRequired) {
    return (callInfo, headers, context) -> {
      String auth = headers.get(AUTH_HEADER);
      if (auth == null || !auth.regionMatches(true, 0, "Negotiate ", 0, 10)) {
        if (kerberosRequired) {
          throw CallStatus.UNAUTHENTICATED
              .withDescription("Missing Kerberos token")
              .toRuntimeException();
        }
        return new KerberosServerMiddleware("anonymous");
      }
      String base64 = auth.substring("Negotiate ".length()).trim();
      try {
        byte[] token = Base64.getDecoder().decode(base64);
        KerberosSpnego.AcceptResult result = KerberosSpnego.acceptToken(token);
        if (!result.established()) {
          throw CallStatus.UNAUTHENTICATED
              .withDescription("Kerberos multi-leg not supported")
              .toRuntimeException();
        }
        String principal = result.principal() != null ? result.principal() : "unknown";
        context.put(PRINCIPAL_HEADER, principal);
        return new KerberosServerMiddleware(principal);
      } catch (IllegalArgumentException ex) {
        throw CallStatus.UNAUTHENTICATED
            .withDescription("Invalid Kerberos token")
            .toRuntimeException();
      } catch (Exception ex) {
        throw CallStatus.UNAUTHENTICATED
            .withDescription("Kerberos validation failed")
            .withCause(ex)
            .toRuntimeException();
      }
    };
  }

  public static Factory clientMiddleware(String targetSpn) {
    Objects.requireNonNull(targetSpn, "targetSpn");
    return callInfo ->
        new FlightClientMiddleware() {
          @Override
          public void onBeforeSendingHeaders(CallHeaders outgoingHeaders) {
            try {
              String token = KerberosSpnego.createInitTokenBase64(targetSpn);
              outgoingHeaders.insert(AUTH_HEADER, "Negotiate " + token);
            } catch (Exception ex) {
              throw CallStatus.UNAUTHENTICATED
                  .withDescription("Kerberos init failed")
                  .withCause(ex)
                  .toRuntimeException();
            }
          }

          @Override
          public void onHeadersReceived(CallHeaders callHeaders) {}

          @Override
          public void onCallCompleted(CallStatus callStatus) {}
        };
  }

  public static final class KerberosServerMiddleware implements FlightServerMiddleware {
    private final String principal;

    private KerberosServerMiddleware(String principal) {
      this.principal = principal;
    }

    @Override
    public void onBeforeSendingHeaders(CallHeaders outgoingHeaders) {
      outgoingHeaders.insert(PRINCIPAL_HEADER, principal);
    }

    @Override
    public void onCallCompleted(CallStatus callStatus) {}

    @Override
    public void onCallErrored(Throwable throwable) {}
  }
}

