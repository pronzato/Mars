package org.pronzato.fabric.test.impala;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/** Standalone Kerberos-enabled Impala JDBC smoke test (no fabric API usage). */
public final class TestImpala {

  private static final String DEFAULT_HOST = "impala-coordinator.example.com";
  private static final int DEFAULT_PORT = 21050;
  private static final String DEFAULT_DATABASE = "default";
  private static final String DEFAULT_SERVICE_NAME = "impala";
  private static final String DEFAULT_CLIENT_PRINCIPAL = "svc_impala@EXAMPLE.COM";
  private static final String DEFAULT_KEYTAB = "C:/security/keytabs/svc_impala.keytab";
  private static final String DEFAULT_KRB5 = "C:/security/krb5.conf";
  private static final String DEFAULT_DRIVER = "com.cloudera.impala.jdbc.Driver";
  private static final String DEFAULT_TABLE = "fabric_test_basic";
  private static final String DEFAULT_REALM = "EXAMPLE.COM";

  private TestImpala() {}

  public static void main(String[] args) throws Exception {
    ImpalaConfig cfg = ImpalaConfig.load();

    applyKrb5(cfg.krb5Conf());
    loadDriver(cfg.driverClass());

    Subject subject = login(cfg);
    Subject.doAs(
        subject,
        (PrivilegedExceptionAction<Void>)
            () -> {
              runQueries(cfg);
              return null;
            });
  }

  private static void runQueries(ImpalaConfig cfg) throws SQLException {
    String jdbcUrl = buildJdbcUrl(cfg);
    Properties props = connectionProperties(cfg);

    System.out.printf(
        "[TestImpala] Connecting with driver=%s url=%s%n", cfg.driverClass(), jdbcUrl);

    try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
      conn.setAutoCommit(true);
      System.out.printf(
          "[TestImpala] Connected. Database=%s AutoCommit=%s%n",
          cfg.database(), conn.getAutoCommit());

      showDatabases(conn);
      exerciseTable(conn, cfg);
    }
  }

  private static void showDatabases(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
      System.out.println("[TestImpala] SHOW DATABASES (first 10):");
      int count = 0;
      while (rs.next() && count < 10) {
        System.out.println("  - " + rs.getString(1));
        count++;
      }
    }
  }

  private static void exerciseTable(Connection conn, ImpalaConfig cfg) throws SQLException {
    String tableName = cfg.tableName() + "_" + Instant.now().toEpochMilli();
    String fqtn = cfg.database() + "." + tableName;

    System.out.println("[TestImpala] Using temp table " + fqtn);
    try {
      executeUpdate(conn, "DROP TABLE IF EXISTS " + fqtn);
      executeUpdate(
          conn,
          "CREATE TABLE "
              + fqtn
              + " (id INT, payload STRING) STORED AS PARQUET");
      executeUpdate(
          conn,
          "INSERT INTO " + fqtn + " VALUES (1, 'hello from TestImpala'), (2, 'kerberos ok')");
      readRows(conn, fqtn);
    } finally {
      try {
        executeUpdate(conn, "DROP TABLE IF EXISTS " + fqtn);
        System.out.println("[TestImpala] Dropped temp table " + fqtn);
      } catch (SQLException ex) {
        System.err.println("[TestImpala] Failed to drop temp table: " + ex.getMessage());
      }
    }
  }

  private static void readRows(Connection conn, String table) throws SQLException {
    String sql = "SELECT id, payload FROM " + table + " ORDER BY id";
    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
      System.out.println("[TestImpala] Query results:");
      while (rs.next()) {
        int id = rs.getInt(1);
        String payload = rs.getString(2);
        System.out.printf("  id=%d payload=%s%n", id, payload);
      }
    }
  }

  private static void executeUpdate(Connection conn, String sql) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
      System.out.println("[TestImpala] Executed: " + sql);
    }
  }

  private static String buildJdbcUrl(ImpalaConfig cfg) {
    if (cfg.jdbcUrl() != null && !cfg.jdbcUrl().isBlank()) {
      return cfg.jdbcUrl().trim();
    }

    StringBuilder url =
        new StringBuilder("jdbc:impala://")
            .append(cfg.host())
            .append(':')
            .append(cfg.port())
            .append('/')
            .append(cfg.database());

    url.append(";AuthMech=1");
    url.append(";KrbRealm=").append(cfg.realm());
    url.append(";KrbHostFQDN=").append(cfg.host());
    url.append(";KrbServiceName=").append(cfg.serviceName());
    url.append(";KrbAuthType=2");
    url.append(";KrbKeytabPath=").append(cfg.keytab());
    if (cfg.krb5Conf() != null) {
      url.append(";Krb5ConfigPath=").append(cfg.krb5Conf());
    }
    url.append(";UseNativeQuery=1");
    if (cfg.ssl()) {
      url.append(";SSL=1;AllowSelfSignedServerCerts=1");
    }
    return url.toString();
  }

  private static Properties connectionProperties(ImpalaConfig cfg) {
    Properties props = new Properties();
    props.setProperty("AuthMech", "1");
    props.setProperty("KrbRealm", cfg.realm());
    props.setProperty("KrbHostFQDN", cfg.host());
    props.setProperty("KrbServiceName", cfg.serviceName());
    props.setProperty("KrbAuthType", "2");
    props.setProperty("KrbKeytabPath", cfg.keytab().toString());
    if (cfg.krb5Conf() != null) {
      props.setProperty("Krb5ConfigPath", cfg.krb5Conf().toString());
    }
    props.setProperty("UseNativeQuery", "1");
    if (cfg.ssl()) {
      props.setProperty("SSL", "1");
      props.setProperty("AllowSelfSignedServerCerts", "1");
    }
    return props;
  }

  private static void loadDriver(String driverClass) {
    try {
      Class.forName(driverClass);
      System.out.println("[TestImpala] Loaded JDBC driver " + driverClass);
    } catch (ClassNotFoundException ex) {
      throw new IllegalStateException(
          "Impala JDBC driver class not found on the classpath: " + driverClass, ex);
    }
  }

  private static Subject login(ImpalaConfig cfg) {
    Map<String, Object> options = new HashMap<>();
    options.put("principal", cfg.clientPrincipal());
    options.put("useKeyTab", "true");
    options.put("keyTab", cfg.keytab().toString());
    options.put("storeKey", "true");
    options.put("doNotPrompt", "true");
    options.put("refreshKrb5Config", "true");
    options.put("isInitiator", "true");

    Configuration jaasConfig =
        new Configuration() {
          @Override
          public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return new AppConfigurationEntry[] {
              new AppConfigurationEntry(
                  "com.sun.security.auth.module.Krb5LoginModule",
                  AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                  options)
            };
          }
        };

    try {
      LoginContext ctx = new LoginContext("impala-test", null, null, jaasConfig);
      ctx.login();
      Subject subject = ctx.getSubject();
      System.out.printf(
          "[TestImpala] Acquired Kerberos subject for %s using %s%n",
          cfg.clientPrincipal(), cfg.keytab().toAbsolutePath());
      return subject;
    } catch (LoginException ex) {
      throw new IllegalStateException("Kerberos login failed: " + ex.getMessage(), ex);
    }
  }

  private static void applyKrb5(java.nio.file.Path krb5Conf) {
    if (krb5Conf != null) {
      System.setProperty("java.security.krb5.conf", krb5Conf.toString());
      System.out.printf("[TestImpala] Using krb5.conf=%s%n", krb5Conf.toAbsolutePath());
      if (!java.nio.file.Files.exists(krb5Conf)) {
        System.err.printf(
            "[TestImpala] WARNING: krb5.conf not found at %s%n", krb5Conf.toAbsolutePath());
      }
    }
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
  }

  private record ImpalaConfig(
      String host,
      int port,
      String database,
      String serviceName,
      String clientPrincipal,
      String realm,
      java.nio.file.Path keytab,
      java.nio.file.Path krb5Conf,
      String driverClass,
      String jdbcUrl,
      String tableName,
      boolean ssl) {

    static ImpalaConfig load() {
      String host = read("FABRIC_TEST_IMPALA_HOST", "fabric.test.impala.host", DEFAULT_HOST);
      int port = readInt("FABRIC_TEST_IMPALA_PORT", "fabric.test.impala.port", DEFAULT_PORT);
      String database =
          read("FABRIC_TEST_IMPALA_DB", "fabric.test.impala.db", DEFAULT_DATABASE);
      String serviceName =
          read("FABRIC_TEST_IMPALA_SERVICE", "fabric.test.impala.service", DEFAULT_SERVICE_NAME);
      String clientPrincipal =
          read(
              "FABRIC_TEST_IMPALA_PRINCIPAL",
              "fabric.test.impala.principal",
              DEFAULT_CLIENT_PRINCIPAL);
      String keytab = read("FABRIC_TEST_IMPALA_KEYTAB", "fabric.test.impala.keytab", DEFAULT_KEYTAB);
      String krb5 = read("FABRIC_TEST_KRB5_CONF", "fabric.test.krb5.conf", DEFAULT_KRB5);
      String driver = read("FABRIC_TEST_IMPALA_DRIVER", "fabric.test.impala.driver", DEFAULT_DRIVER);
      String jdbcUrl = read("FABRIC_TEST_IMPALA_JDBC_URL", "fabric.test.impala.jdbcUrl", "");
      String table = read("FABRIC_TEST_IMPALA_TABLE", "fabric.test.impala.table", DEFAULT_TABLE);
      boolean ssl = readBoolean("FABRIC_TEST_IMPALA_SSL", "fabric.test.impala.ssl", false);
      String realm =
          read("FABRIC_TEST_IMPALA_REALM", "fabric.test.impala.realm", deriveRealm(clientPrincipal));

      return new ImpalaConfig(
          require(host, "host"),
          port,
          require(database, "database"),
          require(serviceName, "serviceName"),
          require(clientPrincipal, "clientPrincipal"),
          require(realm, "realm"),
          requirePath(keytab, "keytab"),
          optionalPath(krb5),
          require(driver, "driver"),
          jdbcUrl == null || jdbcUrl.isBlank() ? null : jdbcUrl.trim(),
          require(table, "table"),
          ssl);
    }
  }

  private static String read(String envKey, String sysKey, String fallback) {
    String sys = System.getProperty(sysKey);
    if (sys != null && !sys.isBlank()) {
      return sys.trim();
    }
    String env = System.getenv(envKey);
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    return fallback;
  }

  private static int readInt(String envKey, String sysKey, int fallback) {
    String value = read(envKey, sysKey, Integer.toString(fallback));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private static boolean readBoolean(String envKey, String sysKey, boolean fallback) {
    String value = read(envKey, sysKey, Boolean.toString(fallback));
    return Boolean.parseBoolean(value);
  }

  private static String require(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required config: " + label);
    }
    return value.trim();
  }

  private static java.nio.file.Path requirePath(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required path: " + label);
    }
    return java.nio.file.Paths.get(value.trim());
  }

  private static java.nio.file.Path optionalPath(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return java.nio.file.Paths.get(value.trim());
  }

  private static String deriveRealm(String principal) {
    if (principal != null) {
      int at = principal.indexOf('@');
      if (at > 0 && at < principal.length() - 1) {
        return principal.substring(at + 1);
      }
    }
    return DEFAULT_REALM;
  }
}
