package org.pronzato.fabric.api.kerberos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified Kerberos facade used by Fabric services and clients (AMPS, gRPC, Arrow Flight) across
 * bare-metal and OpenShift deployments.
 *
 * <p>The default usage expects:
 *
 * <ul>
 *   <li>A {@code krb5.conf} resource packaged with Fabric (read from the classpath).
 *   <li>A keytab provided either as a {@link Path} or raw bytes.
 *   <li>An optional explicit principal; if omitted, the current user name is combined with the
 *       {@code FABRIC.COM} realm.
 * </ul>
 *
 * Example:
 *
 * <pre>{@code
 * KerberosApi api =
 *     KerberosApi.builder()
 *         .withKeytabPath(Path.of("/etc/security/keytabs/fabric.keytab"))
 *         .build()
 *         .start();
 *
 * HAClient client = api.doAs(() -> {
 *   HAClient amps = new HAClient("fabric-client");
 *   amps.connect("tcp://amps1:9007/amps/json");
 *   amps.logon();
 *   return amps;
 * });
 * }</pre>
 */
public final class KerberosApi implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(KerberosApi.class);
  private static final String DEFAULT_KRB5_RESOURCE = "krb5.conf";
  private static final String DEFAULT_REALM = "CORP.FABRIC.COM";
  private static final Duration DEFAULT_RELOGIN = Duration.ofMinutes(30);
  private static final Oid SPNEGO_OID;
  private static final String LOGIN_CONTEXT = "FabricKerberosApp";

  static {
    try {
      SPNEGO_OID = new Oid("1.3.6.1.5.5.2");
    } catch (GSSException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Returns a new builder with sensible defaults. */
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String krb5Resource = DEFAULT_KRB5_RESOURCE;
    private Path krb5Path;
    private byte[] krb5Bytes;
    private Path keytabPath;
    private byte[] keytabBytes;
    private String principal;
    private Duration reloginEvery = DEFAULT_RELOGIN;
    private String realm;
    private String kdc;

    /** Use a custom classpath resource for krb5.conf (defaults to {@code krb5.conf}). */
    public Builder withKrb5Resource(String resourceName) {
      this.krb5Resource = resourceName;
      this.krb5Path = null;
      this.krb5Bytes = null;
      return this;
    }

    /** Use a filesystem krb5.conf instead of the bundled resource. */
    public Builder withKrb5Path(Path path) {
      this.krb5Path = path;
      this.krb5Bytes = null;
      this.krb5Resource = null;
      return this;
    }

    /** Provide krb5.conf contents directly (e.g., secret mount). */
    public Builder withKrb5Bytes(byte[] bytes) {
      this.krb5Bytes = bytes != null ? bytes.clone() : null;
      this.krb5Path = null;
      this.krb5Resource = null;
      return this;
    }

    /** Provide the keytab location on disk. */
    public Builder withKeytabPath(Path path) {
      this.keytabPath = path;
      return this;
    }

    /** Provide the keytab content directly (e.g., mounted secret). */
    public Builder withKeytabBytes(byte[] bytes) {
      this.keytabBytes = bytes != null ? bytes.clone() : null;
      return this;
    }

    /** Override the Kerberos principal to authenticate as. */
    public Builder withPrincipal(String value) {
      this.principal = value;
      return this;
    }

    /** Control the relogin cadence (defaults to 30 minutes). */
    public Builder withReloginEvery(Duration duration) {
      if (duration == null) {
        this.reloginEvery = DEFAULT_RELOGIN;
      } else if (duration.isZero() || duration.isNegative()) {
        this.reloginEvery = Duration.ZERO;
      } else {
        this.reloginEvery = duration;
      }
      return this;
    }

    public Builder withRealm(String value) {
      if (value != null && !value.isBlank()) {
        this.realm = value.trim();
      }
      return this;
    }

    public Builder withKdc(String value) {
      if (value != null && !value.isBlank()) {
        this.kdc = value.trim();
      }
      return this;
    }

    public KerberosApi build() {
      return new KerberosApi(
          new Config(
              krb5Resource,
              krb5Path,
              krb5Bytes,
              keytabPath,
              keytabBytes,
              principal,
              reloginEvery,
              realm,
              kdc));
    }
  }

  private final Config config;
  private final ScheduledExecutorService reloginExecutor =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "fabric-kerberos-relogin");
            t.setDaemon(true);
            return t;
          });

  private volatile boolean started;
  private volatile LoginContext loginContext;
  private volatile Subject subject;
  private volatile Path extractedKrb5;
  private volatile Path extractedKeytab;
  private volatile ScheduledFuture<?> reloginTask;
  private volatile Path resolvedKrb5Path;
  private volatile Path resolvedKeytabPath;
  private volatile String resolvedPrincipal;

  private KerberosApi(Config config) {
    this.config = config;
  }

  /** Starts Kerberos login; safe to call once. */
  public synchronized KerberosApi start() {
    if (started) {
      return this;
    }
    try {
      Path krb5File = resolveKrb5Config();
      Path keytabFile = resolveKeytab();
      String principal = resolvePrincipal();

      applySystemProperties(krb5File, config.realm, config.kdc);
      installLoginConfig(principal, keytabFile);

      loginContext = new LoginContext(LOGIN_CONTEXT, NO_PROMPT_HANDLER);
      loginContext.login();
      subject = loginContext.getSubject();

      if (!config.reloginEvery.isZero() && !config.reloginEvery.isNegative()) {
        long seconds = Math.max(60, config.reloginEvery.getSeconds());
        reloginTask = reloginExecutor.scheduleAtFixedRate(this::relogin, seconds, seconds, TimeUnit.SECONDS);
        LOGGER.info("[Kerberos] Scheduled re-login every {} seconds", seconds);
      }

      LOGGER.info(
          "[Kerberos] Login successful principal={} keytab={} krb5={}",
          principal,
          keytabFile.toAbsolutePath(),
          krb5File.toAbsolutePath());
      started = true;
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Kerberos start failed: " + e.getMessage(), e);
    }
  }

  /** Executes the supplied action as the current Kerberos subject. */
  @SuppressWarnings("removal")
  public <T> T doAs(Supplier<T> action) {
    Objects.requireNonNull(action, "action");
    ensureStarted();
    return Subject.doAs(subject, (PrivilegedAction<T>) action::get);
  }

  /**
   * Builds a Negotiate header for the target service (e.g., {@code AMPS}, {@code HTTP}, {@code
   * GRPC}).
   */
  @SuppressWarnings("removal")
  public String buildNegotiateHeader(String serviceClass, String hostFqdn) {
    Objects.requireNonNull(serviceClass, "serviceClass");
    Objects.requireNonNull(hostFqdn, "hostFqdn");
    ensureStarted();
    return Subject.doAs(
        subject,
        (PrivilegedAction<String>)
            () -> {
              try {
                GSSManager manager = GSSManager.getInstance();
                String spn = serviceClass + "/" + hostFqdn;
                GSSName serverName = manager.createName(spn, GSSName.NT_HOSTBASED_SERVICE);
                GSSContext ctx =
                    manager.createContext(serverName, SPNEGO_OID, null, GSSContext.DEFAULT_LIFETIME);
                ctx.requestMutualAuth(true);
                ctx.requestCredDeleg(true);
                byte[] token = ctx.initSecContext(new byte[0], 0, 0);
                if (token == null) {
                  token = new byte[0];
                }
                return "Negotiate " + Base64.getEncoder().encodeToString(token);
              } catch (GSSException e) {
                throw new RuntimeException("Failed to create Negotiate header: " + e.getMessage(), e);
              }
            });
  }

  /** Convenience wrapper for HTTP-based protocols. */
  public String buildHttpNegotiateHeader(String hostFqdn) {
    return buildNegotiateHeader("HTTP", hostFqdn);
  }

  /**
   * Accepts an inbound SPNEGO token (e.g., on a Fabric service) and returns the negotiation result.
   */
  public SpnegoAcceptResult acceptSpnegoToken(byte[] token) {
    Objects.requireNonNull(token, "token");
    ensureStarted();
    try {
      GSSManager manager = GSSManager.getInstance();
      GSSContext context = manager.createContext((GSSCredential) null);
      byte[] response = context.acceptSecContext(token, 0, token.length);
      boolean established = context.isEstablished();
      String principal = context.getSrcName() != null ? context.getSrcName().toString() : null;
      return new SpnegoAcceptResult(principal, response, established);
    } catch (GSSException e) {
      throw new RuntimeException("Failed to accept SPNEGO token: " + e.getMessage(), e);
    }
  }

  @Override
  public synchronized void close() {
    if (!started) {
      return;
    }
    if (reloginTask != null) {
      reloginTask.cancel(true);
    }
    reloginExecutor.shutdownNow();
    if (loginContext != null) {
      try {
        loginContext.logout();
      } catch (Exception ignore) {
      }
    }
    deleteIfExists(extractedKrb5);
    deleteIfExists(extractedKeytab);
    resolvedKrb5Path = null;
    resolvedKeytabPath = null;
    resolvedPrincipal = null;
    started = false;
  }

  private void relogin() {
    try {
      if (loginContext != null) {
        loginContext.logout();
        loginContext.login();
        subject = loginContext.getSubject();
        String principal =
            (subject != null && !subject.getPrincipals().isEmpty())
                ? subject.getPrincipals().iterator().next().toString()
                : "<unknown>";
        LOGGER.info("[Kerberos] Re-login successful for principal {}", principal);
      }
    } catch (Exception e) {
      LOGGER.warn("[Kerberos] Re-login failed", e);
    }
  }

  private Path resolveKrb5Config() throws IOException {
    if (config.krb5Path != null) {
      resolvedKrb5Path = config.krb5Path;
      LOGGER.info("[Kerberos] Using krb5 configuration path {}", resolvedKrb5Path.toAbsolutePath());
      return resolvedKrb5Path;
    }
    if (config.krb5Bytes != null && config.krb5Bytes.length > 0) {
      extractedKrb5 = writeTempFile("fabric-krb5-", ".conf", config.krb5Bytes);
      resolvedKrb5Path = extractedKrb5;
      LOGGER.info("[Kerberos] Using provided krb5.conf bytes extracted to {}", resolvedKrb5Path.toAbsolutePath());
      return resolvedKrb5Path;
    }
    String resource =
        (config.krb5Resource == null || config.krb5Resource.isBlank())
            ? DEFAULT_KRB5_RESOURCE
            : config.krb5Resource;
    try (InputStream in = KerberosApi.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalArgumentException("krb5 resource not found on classpath: " + resource);
      }
      extractedKrb5 = writeTempFile("fabric-krb5-", ".conf", in.readAllBytes());
      resolvedKrb5Path = extractedKrb5;
      LOGGER.info(
          "[Kerberos] Using classpath krb5 resource {} extracted to {}",
          resource,
          resolvedKrb5Path.toAbsolutePath());
      return resolvedKrb5Path;
    }
  }

  private Path resolveKeytab() throws IOException {
    if (config.keytabPath != null) {
      resolvedKeytabPath = config.keytabPath;
      LOGGER.info("[Kerberos] Using keytab path {}", resolvedKeytabPath.toAbsolutePath());
      return resolvedKeytabPath;
    }
    if (config.keytabBytes != null && config.keytabBytes.length > 0) {
      extractedKeytab = writeTempFile("fabric-keytab-", ".kt", config.keytabBytes);
      resolvedKeytabPath = extractedKeytab;
      LOGGER.info("[Kerberos] Using provided keytab bytes extracted to {}", resolvedKeytabPath.toAbsolutePath());
      return resolvedKeytabPath;
    }
    throw new IllegalArgumentException("Keytab path or keytab bytes are required");
  }

  private String resolvePrincipal() {
    if (config.principal != null && !config.principal.isBlank()) {
      resolvedPrincipal = config.principal;
      LOGGER.info("[Kerberos] Using explicit principal {}", resolvedPrincipal);
      return resolvedPrincipal;
    }
    String user = System.getProperty("user.name");
    if (user == null || user.isBlank()) {
      LOGGER.error("[Kerberos] System property user.name is blank; unable to derive default principal");
      throw new IllegalStateException("Unable to determine current user for Kerberos principal");
    }
    String normalized = user.replace('\\', '/').trim();
    String resolved = normalized + "@" + DEFAULT_REALM;
    LOGGER.info(
        "[Kerberos] Using derived principal {} from system user '{}' and default realm {}",
        resolved,
        user,
        DEFAULT_REALM);
    resolvedPrincipal = resolved;
    return resolvedPrincipal;
  }

  private static void applySystemProperties(Path krb5File, String realm, String kdc) {
    String absolute = krb5File.toAbsolutePath().toString();
    System.setProperty("java.security.krb5.conf", absolute);
    System.setProperty("sun.security.krb5.config", absolute);
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    if (realm != null && !realm.isBlank()) {
      System.setProperty("java.security.krb5.realm", realm.trim());
    }
    if (kdc != null && !kdc.isBlank()) {
      System.setProperty("java.security.krb5.kdc", kdc.trim());
    }
    LOGGER.info("[Kerberos] Set system krb5.conf to {}", krb5File.toAbsolutePath());
  }

  private static void installLoginConfig(String principal, Path keytab) {
    Objects.requireNonNull(keytab, "keytab");
    AppConfigurationEntry entry =
        new AppConfigurationEntry(
            "com.sun.security.auth.module.Krb5LoginModule",
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
            buildLoginOptions(principal, keytab));
    Configuration.setConfiguration(new SingleEntryLoginConfig(LOGIN_CONTEXT, entry));
    LOGGER.info(
        "[Kerberos] Installed Kerberos login configuration '{}' for principal {} with keytab {}",
        LOGIN_CONTEXT,
        principal,
        keytab.toAbsolutePath());
  }

  private static java.util.Map<String, Object> buildLoginOptions(
      String principal, Path keytabPath) {
    java.util.Map<String, Object> options = new java.util.HashMap<>();
    options.put("useKeyTab", "true");
    options.put("keyTab", keytabPath.toAbsolutePath().toString());
    options.put("principal", principal.toUpperCase(Locale.ROOT));
    options.put("storeKey", "true");
    options.put("doNotPrompt", "true");
    options.put("useTicketCache", "false");
    options.put("renewTGT", "false");
    options.put("refreshKrb5Config", "true");
    options.put("isInitiator", "true");
    options.put("debug", Boolean.toString(LOGGER.isDebugEnabled()));
    return options;
  }

  private static Path writeTempFile(String prefix, String suffix, byte[] content) throws IOException {
    Path file = Files.createTempFile(prefix, suffix);
    Files.write(file, content, StandardOpenOption.TRUNCATE_EXISTING);
    return file;
  }

  private static void deleteIfExists(Path file) {
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file);
    } catch (IOException ignore) {
    }
  }

  private void ensureStarted() {
    if (!started || subject == null) {
      throw new IllegalStateException("KerberosApi has not been started yet");
    }
  }

  /** Returns the principal in use after {@link #start()}. */
  public String principal() {
    ensureStarted();
    return resolvedPrincipal;
  }

  /** Returns the keytab path currently in use after {@link #start()}. */
  public Path keytabPath() {
    ensureStarted();
    return resolvedKeytabPath;
  }

  /** Returns the krb5.conf path currently in use after {@link #start()}. */
  public Path krb5Path() {
    ensureStarted();
    return resolvedKrb5Path;
  }

  private static final class Config {
    final String krb5Resource;
    final Path krb5Path;
    final byte[] krb5Bytes;
    final Path keytabPath;
    final byte[] keytabBytes;
    final String principal;
    final Duration reloginEvery;
    final String realm;
    final String kdc;

    Config(
        String krb5Resource,
        Path krb5Path,
        byte[] krb5Bytes,
        Path keytabPath,
        byte[] keytabBytes,
        String principal,
        Duration reloginEvery,
        String realm,
        String kdc) {
      this.krb5Resource = krb5Resource;
      this.krb5Path = krb5Path;
      this.krb5Bytes = krb5Bytes != null ? krb5Bytes.clone() : null;
      this.keytabPath = keytabPath;
      this.keytabBytes = keytabBytes != null ? keytabBytes.clone() : null;
      this.principal = principal;
      this.reloginEvery = reloginEvery == null ? DEFAULT_RELOGIN : reloginEvery;
      this.realm = realm;
      this.kdc = kdc;
    }
  }

  /** Result from accepting a SPNEGO token. */
  public static final class SpnegoAcceptResult {
    private final String principal;
    private final byte[] responseToken;
    private final boolean established;

    SpnegoAcceptResult(String principal, byte[] responseToken, boolean established) {
      this.principal = principal;
      this.responseToken = responseToken != null ? responseToken.clone() : null;
      this.established = established;
    }

    public String principal() {
      return principal;
    }

    public byte[] responseToken() {
      return responseToken != null ? responseToken.clone() : null;
    }

    public boolean established() {
      return established;
    }
  }

  private static final class SingleEntryLoginConfig extends Configuration {
    private final String name;
    private final AppConfigurationEntry[] entries;

    SingleEntryLoginConfig(String name, AppConfigurationEntry entry) {
      this.name = name;
      this.entries = new AppConfigurationEntry[] {entry};
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
      return Objects.equals(this.name, name) ? entries : null;
    }
  }

  private static final CallbackHandler NO_PROMPT_HANDLER = new NoPromptCallbackHandler();

  private static final class NoPromptCallbackHandler implements CallbackHandler {
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      if (callbacks == null || callbacks.length == 0) {
        return;
      }
      LOGGER.warn(
          "[Kerberos] Blocking interactive callback of type {} to prevent password prompt.",
          callbacks[0].getClass().getSimpleName());
      throw new UnsupportedCallbackException(
          callbacks[0], "Kerberos interactive prompts are disabled for Fabric clients.");
    }
  }
}

