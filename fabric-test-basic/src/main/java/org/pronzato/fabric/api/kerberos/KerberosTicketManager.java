package org.pronzato.fabric.api.kerberos;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

/**
 * Centralizes Kerberos login + ticket refresh bookkeeping across clients.
 *
 * <p>Cache keys are mapped to logical principals/roles (e.g. {@code "client"},
 * {@code "orders-app-client"}, {@code "positions-service"}), not protocol names. A single Subject
 * (per cache key) holds one TGT and multiple service tickets as the client touches gRPC, Arrow,
 * AMPS, HTTP, etc.
 */
public final class KerberosTicketManager {

  private static final KerberosTicketManager INSTANCE = new KerberosTicketManager();

  private static final String DEFAULT_CLIENT_CACHE_KEY = "client";
  private static final String DEFAULT_SERVICE_CACHE_KEY = "service";

  private final ConcurrentHashMap<String, Subject> subjectCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, KerberosLogin.Refresher> refreshers =
      new ConcurrentHashMap<>();

  private KerberosTicketManager() {}

  // DESIGN NOTE:
  //  - cacheKey represents a logical Kerberos principal/role (e.g. "client", "positions-app-client").
  //  - A single Subject (per cacheKey) holds one TGT plus multiple service tickets across protocols.
  //  - DO NOT use per-protocol cache keys like "grpc-client" or "http-client".
  //  - Reuse one cache key for the client role; servers should use ensureServiceLogin(...) variants.

  /** Returns the singleton Kerberos ticket manager for this JVM. */
  public static KerberosTicketManager getInstance() {
    return INSTANCE;
  }

  /**
   * Ensures a Kerberos client/initiator login exists for the given logical principal/role.
   *
   * <p>cacheKey should describe the principal (e.g. {@code "client"} or {@code "orders-app-client"})
   * rather than the protocol (avoid labels such as {@code "grpc-client"} or {@code "arrow-client"}).
   * Kerberos will manage multiple service tickets inside the single cached {@link Subject}.
   */
  public static Subject ensureLogin(String cacheKey, KerberosLogin.Config cfg) {
    boolean autoRefresh = cfg != null && cfg.autoRefresh;
    Duration refreshPeriod = cfg != null ? cfg.refreshPeriod : null;
    return ensureLogin(cacheKey, cfg, autoRefresh, refreshPeriod);
  }

  /** Static convenience wrapper that delegates to {@link #getInstance()} (client initiator role). */
  public static Subject ensureLogin(
      String cacheKey, KerberosLogin.Config cfg, boolean autoRefresh, Duration refreshPeriod) {
    return getInstance().login(cacheKey, cfg, autoRefresh, refreshPeriod);
  }

  /**
   * Static convenience wrapper for service/acceptor login.
   *
   * <p>Cache keys should differentiate logical service principals (e.g. {@code "service"},
   * {@code "positions-service"}) rather than protocols.
   */
  public static Subject ensureServiceLogin(String cacheKey, KerberosLogin.Config cfg) {
    boolean autoRefresh = cfg != null && cfg.autoRefresh;
    Duration refreshPeriod = cfg != null ? cfg.refreshPeriod : null;
    return ensureServiceLogin(cacheKey, cfg, autoRefresh, refreshPeriod);
  }

  /** Static convenience wrapper for service/acceptor login with explicit refresh settings. */
  public static Subject ensureServiceLogin(
      String cacheKey, KerberosLogin.Config cfg, boolean autoRefresh, Duration refreshPeriod) {
    return getInstance().loginService(cacheKey, cfg, autoRefresh, refreshPeriod);
  }

  /**
   * Use this for all client-side Kerberos tests; one subject is reused across all outbound
   * protocols (gRPC, Arrow, AMPS, HTTP, etc.).
   */
  public static Subject ensureDefaultClientLogin(KerberosLogin.Config cfg) {
    return ensureLogin(DEFAULT_CLIENT_CACHE_KEY, cfg);
  }

  /**
   * Use this for Kerberos-enabled test servers. If multiple service principals are needed, supply
   * distinct cache keys per principal (not per protocol).
   */
  public static Subject ensureDefaultServiceLogin(KerberosLogin.Config cfg) {
    return ensureServiceLogin(DEFAULT_SERVICE_CACHE_KEY, cfg);
  }

  /** Instance method allowing callers to reuse the singleton while passing a reference explicitly. */
  public Subject login(
      String cacheKey, KerberosLogin.Config cfg, boolean autoRefresh, Duration refreshPeriod) {
    return loginInternal(cacheKey, cfg, autoRefresh, refreshPeriod, true);
  }

  /** Service/acceptor variant for callers that need to authenticate as a service principal. */
  public Subject loginService(
      String cacheKey, KerberosLogin.Config cfg, boolean autoRefresh, Duration refreshPeriod) {
    return loginInternal(cacheKey, cfg, autoRefresh, refreshPeriod, false);
  }

  private Subject loginInternal(
      String cacheKey,
      KerberosLogin.Config cfg,
      boolean autoRefresh,
      Duration refreshPeriod,
      boolean isInitiator) {
    Objects.requireNonNull(cacheKey, "cacheKey");
    Objects.requireNonNull(cfg, "cfg");

    cfg.autoRefresh(autoRefresh).refreshEvery(refreshPeriod);
    KerberosLogin.Config effective = cfg.copy();
    Subject subject =
        subjectCache.computeIfAbsent(
            cacheKey,
            key -> {
              LoginContext ctx =
                  isInitiator
                      ? KerberosLogin.loginAsClient(effective)
                      : KerberosLogin.loginAsService(effective);
              return ctx != null ? ctx.getSubject() : null;
            });

    updateRefresher(cacheKey, effective, isInitiator);
    return subject;
  }

  private void updateRefresher(
      String cacheKey, KerberosLogin.Config cfg, boolean isInitiator) {
    boolean canAutoRefresh = cfg.autoRefresh && hasKeytab(cfg);
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
          return KerberosLogin.startAutoRefresh(cfg, effective, isInitiator);
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



