package org.pronzato.fabric.api.kerberos;

import java.util.Base64;
import java.util.Objects;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/** Helper utilities for SPNEGO/Kerberos token creation and validation. */
public final class KerberosSpnego {

  private static final Oid SPNEGO_OID;

  static {
    try {
      SPNEGO_OID = new Oid("1.3.6.1.5.5.2");
    } catch (GSSException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private KerberosSpnego() {}

  /**
   * Creates a base64 encoded SPNEGO init token targeting the supplied service principal name.
   *
   * @param targetSpn e.g. "amps/hostname@REALM"
   */
  public static String createInitTokenBase64(String targetSpn) throws GSSException {
    return Base64.getEncoder().encodeToString(createInitToken(targetSpn));
  }

  /**
   * Creates the raw SPNEGO init token targeting the supplied service principal name.
   */
  public static byte[] createInitToken(String targetSpn) throws GSSException {
    Objects.requireNonNull(targetSpn, "targetSpn");
    GSSManager manager = GSSManager.getInstance();
    GSSName serverName = manager.createName(targetSpn, GSSName.NT_USER_NAME);
    GSSContext context =
        manager.createContext(serverName, SPNEGO_OID, null, GSSContext.DEFAULT_LIFETIME);
    context.requestMutualAuth(true);
    context.requestCredDeleg(true);
    byte[] token = context.initSecContext(new byte[0], 0, 0);
    return token != null ? token : new byte[0];
  }

  /**
   * Accepts an incoming SPNEGO token and returns the negotiation result.
   */
  public static AcceptResult acceptToken(byte[] token) throws GSSException {
    Objects.requireNonNull(token, "token");
    GSSManager manager = GSSManager.getInstance();
    GSSContext context = manager.createContext((GSSCredential) null);
    byte[] response = context.acceptSecContext(token, 0, token.length);
    boolean established = context.isEstablished();
    String principal = context.getSrcName() != null ? context.getSrcName().toString() : null;
    return new AcceptResult(principal, response, established);
  }

  /** Outcome of accepting a SPNEGO token. */
  public static final class AcceptResult {
    private final String principal;
    private final byte[] responseToken;
    private final boolean established;

    AcceptResult(String principal, byte[] responseToken, boolean established) {
      this.principal = principal;
      this.responseToken = responseToken != null ? responseToken.clone() : null;
      this.established = established;
    }

    /** Authenticated Kerberos principal (may be null). */
    public String principal() {
      return principal;
    }

    /** Response token to return to the client (for multi-leg negotiations), may be null. */
    public byte[] responseToken() {
      return responseToken != null ? responseToken.clone() : null;
    }

    /** Whether the security context has been fully established. */
    public boolean established() {
      return established;
    }
  }
}



