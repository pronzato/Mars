package org.pronzato.fabric.api.kerberos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** Minimal Kerberos (GSSAPI) bootstrap for AMPS and other Java clients. */
public final class KerberosLogin {
  private static final Logger LOGGER = LoggerFactory.getLogger(KerberosLogin.class);
  private static final String LOGIN_CONTEXT = "FabricKerberosLogin";


  /** Configuration for Kerberos keytab login. */
  public static final class Config {
    public boolean enabled;
    public String principal; // e.g. "svc_amps@YOUR.REALM"
    public String keytabPath; // path to keytab (optional if keytabData is provided)
    public byte[] keytabData;
    public String realm; // optional; else pick from krb5.conf
    public String kdc; // optional; else pick from krb5.conf
    public boolean autoRefresh = true;
    public Duration refreshPeriod = Duration.ofHours(6);

    // Optional krb5 overrides
    public String krb5ConfigPath;
    public byte[] krb5ConfigData;

    public Config enabled(boolean value) {
      this.enabled = value;
      return this;
    }

    public Config principal(String value) {
      this.principal = value;
      return this;
    }

    public Config keytab(String value) {
      this.keytabPath = value;
      return this;
    }

    public Config keytabBytes(byte[] value) {
      this.keytabData = value != null ? value.clone() : null;
      return this;
    }

    public Config realm(String value) {
      this.realm = value;
      return this;
    }

    public Config kdc(String value) {
      this.kdc = value;
      return this;
    }

    public Config autoRefresh(boolean value) {
      this.autoRefresh = value;
      return this;
    }

    public Config refreshEvery(Duration period) {
      if (period != null && !period.isZero() && !period.isNegative()) {
        this.refreshPeriod = period;
      }
      return this;
    }

    public Config krb5ConfigPath(String path) {
      this.krb5ConfigPath = path;
      return this;
    }

    public Config krb5ConfigData(byte[] data) {
      this.krb5ConfigData = data != null ? data.clone() : null;
      return this;
    }

    public Config copy() {
      Config copy = new Config();
      copy.enabled = enabled;
      copy.principal = principal;
      copy.keytabPath = keytabPath;
      copy.keytabData = keytabData != null ? keytabData.clone() : null;
      copy.realm = realm;
      copy.kdc = kdc;
      copy.autoRefresh = autoRefresh;
      copy.refreshPeriod = refreshPeriod;
      copy.krb5ConfigPath = krb5ConfigPath;
      copy.krb5ConfigData = krb5ConfigData != null ? krb5ConfigData.clone() : null;
      return copy;
    }
  }

  public static LoginContext loginAsClient(Config cfg) {
    return setupAndLogin(cfg, true);
  }

  public static LoginContext loginAsService(Config cfg) {
    return setupAndLogin(cfg, false);
  }

  /** Configure system properties and log in if enabled. Safe to call multiple times. */
  private static LoginContext setupAndLogin(Config cfg, boolean isInitiator) {
    if (cfg == null || !cfg.enabled) {
      return null;
    }

    cfg.krb5ConfigPath = ensureConfigFile(cfg.krb5ConfigPath, cfg.krb5ConfigData, "krb5-", ".conf");
    cfg.keytabPath = ensureConfigFile(cfg.keytabPath, cfg.keytabData, "fabric-", ".keytab");

    if (notBlank(cfg.krb5ConfigPath)) {
      System.setProperty("java.security.krb5.conf", cfg.krb5ConfigPath);
      System.setProperty("sun.security.krb5.config", cfg.krb5ConfigPath);
    }

    if (notBlank(cfg.realm)) {
      System.setProperty("java.security.krb5.realm", cfg.realm);
    }
    if (notBlank(cfg.kdc)) {
      System.setProperty("java.security.krb5.kdc", cfg.kdc);
    }

    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

    final String principal =
        Objects.requireNonNull(cfg.principal, "Kerberos principal is required");
    final String keytab =
        Objects.requireNonNull(cfg.keytabPath, "Kerberos keytabPath or keytabData is required");
    KerberosPrincipal kp = new KerberosPrincipal(principal);

    AppConfigurationEntry.LoginModuleControlFlag ctl =
        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;

    Map<String, String> options = new HashMap<>();
    options.put("refreshKrb5Config", "true");
    options.put("doNotPrompt", "true");
    options.put("renewTGT", "false");
    options.put("useKeyTab", "true");
    options.put("keyTab", keytab);
    options.put("storeKey", "true");
    options.put("useTicketCache", "false");
    options.put("principal", principal);
    options.put("isInitiator", Boolean.toString(isInitiator));

    AppConfigurationEntry entry =
        new AppConfigurationEntry(
            "com.sun.security.auth.module.Krb5LoginModule", ctl, options);
    Configuration loginConfig = new SingleEntryLoginConfig(LOGIN_CONTEXT, entry);
    Configuration.setConfiguration(loginConfig);

    try {
      Subject subject =
          new Subject(
              false,
              Collections.singleton(kp),
              Collections.emptySet(),
              Collections.emptySet());
      LoginContext lc = new LoginContext(LOGIN_CONTEXT, subject, NO_PROMPT_HANDLER);
      lc.login();
      LOGGER.info("[Kerberos] Login OK as {} (keytab)", principal);
      return lc;
    } catch (Exception e) {
      throw new RuntimeException("Kerberos login failed for " + principal + ": " + e, e);
    }
  }

  public static Refresher startAutoRefresh(Config cfg, Duration period) {
    return startAutoRefresh(cfg, period, true);
  }

  public static Refresher startAutoRefresh(Config cfg, Duration period, boolean isInitiator) {
    if (cfg == null || !cfg.enabled) {
      return null;
    }
    Duration effective =
        (period == null || period.isZero() || period.isNegative())
            ? Duration.ofHours(6)
            : period;
    return new Refresher(cfg.copy(), effective, isInitiator);
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static String ensureConfigFile(String existingPath, byte[] data, String prefix, String suffix) {
    if (notBlank(existingPath)) {
      return existingPath;
    }
    if (data == null || data.length == 0) {
      return null;
    }
    try {
      Path temp = Files.createTempFile(prefix, suffix);
      Files.write(temp, data);
      temp.toFile().deleteOnExit();
      return temp.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to materialize temporary Kerberos config", e);
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
      return this.name.equals(name) ? entries : null;
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

  public static final class Refresher implements AutoCloseable {
    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> future;
    private final Config config;
    private final Duration refreshPeriod;
    private final boolean isInitiator;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    Refresher(Config config, Duration refreshPeriod, boolean isInitiator) {
      this.config = config.copy();
      this.refreshPeriod = refreshPeriod;
      this.isInitiator = isInitiator;
      long refreshSeconds = Math.max(60, refreshPeriod.getSeconds());
      this.scheduler =
          Executors.newSingleThreadScheduledExecutor(
              new RefreshThreadFactory(config.principal));
      this.future =
          scheduler.scheduleAtFixedRate(
              this::refreshSafe, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
    }

    private void refreshSafe() {
      if (closed.get()) {
        return;
      }
      try {
        setupAndLogin(config, isInitiator);
      } catch (RuntimeException e) {
        LOGGER.error("[KerberosLogin] Periodic refresh failed", e);
      }
    }

    public Duration refreshPeriod() {
      return refreshPeriod;
    }

    public boolean isClosed() {
      return closed.get();
    }

    @Override
    public void close() {
      if (closed.compareAndSet(false, true)) {
        future.cancel(true);
        scheduler.shutdownNow();
      }
    }
  }

  private static final class RefreshThreadFactory implements ThreadFactory {
    private final String label;

    RefreshThreadFactory(String label) {
      if (label == null || label.isBlank()) {
        this.label = "Kerberos";
      } else {
        this.label = label;
      }
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "KerberosLogin-Refresh-" + label);
      t.setDaemon(true);
      return t;
    }
  }

  private KerberosLogin() {}
}

