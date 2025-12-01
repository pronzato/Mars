package org.pronzato.fabric.api.kerberos;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Centralizes Kerberos login + ticket refresh bookkeeping across clients. */
public final class KerberosTicketManager {

  private static final KerberosTicketManager INSTANCE = new KerberosTicketManager();

  private final ConcurrentHashMap<String, KerberosLogin.Refresher> refreshers =
      new ConcurrentHashMap<>();

  private KerberosTicketManager() {}

  /** Returns the singleton Kerberos ticket manager for this JVM. */
  public static KerberosTicketManager getInstance() {
    return INSTANCE;
  }

  /** Static convenience wrapper that delegates to {@link #getInstance()}. */
  public static void ensureLogin(
      String cacheKey, KerberosLogin.Config cfg, boolean autoRefresh, Duration refreshPeriod) {
    getInstance().login(cacheKey, cfg, autoRefresh, refreshPeriod);
  }

  /** Instance method allowing callers to reuse the singleton while passing a reference explicitly. */
  public void login(String cacheKey, KerberosLogin.Config cfg, boolean autoRefresh, Duration refreshPeriod) {
    Objects.requireNonNull(cacheKey, "cacheKey");
    Objects.requireNonNull(cfg, "cfg");

    cfg.autoRefresh(autoRefresh).refreshEvery(refreshPeriod);
    KerberosLogin.setupAndLogin(cfg);

    boolean canAutoRefresh = autoRefresh && hasKeytab(cfg);
    refreshers.compute(
        cacheKey,
        (key, existing) -> {
          if (!canAutoRefresh) {
            closeQuietly(existing);
            return null;
          }
          Duration effective = sanitize(cfg.refreshPeriod);
          if (existing != null
              && !existing.isClosed()
              && effective.equals(existing.refreshPeriod())) {
            return existing;
          }
          closeQuietly(existing);
          return KerberosLogin.startAutoRefresh(cfg, effective);
        });
  }

  private static boolean hasKeytab(KerberosLogin.Config cfg) {
    return cfg.keytabPath != null && !cfg.keytabPath.isBlank();
  }

  private static Duration sanitize(Duration value) {
    if (value == null || value.isZero() || value.isNegative()) {
      return Duration.ofHours(6);
    }
    return value;
  }

  private static void closeQuietly(KerberosLogin.Refresher refresher) {
    if (refresher != null) {
      try {
        refresher.close();
      } catch (Exception ignore) {
      }
    }
  }
}



