package org.pronzato.fabric.test.hdfs;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.time.Instant;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

/**
 * Standalone Kerberos-enabled HDFS smoke test that lists, writes, and deletes a file.
 *
 * <p>Configuration (env var or -D system property):
 *
 * <ul>
 *   <li>{@code FABRIC_TEST_HDFS_URI} / {@code fabric.test.hdfs.uri}: namenode URI (fs.defaultFS)
 *   <li>{@code FABRIC_TEST_HDFS_CLIENT_PRINCIPAL} / {@code fabric.test.hdfs.clientPrincipal}
 *   <li>{@code FABRIC_TEST_HDFS_KEYTAB} / {@code fabric.test.hdfs.keytab}
 *   <li>{@code FABRIC_TEST_HDFS_NN_PRINCIPAL} / {@code fabric.test.hdfs.namenodePrincipal}
 *   <li>{@code FABRIC_TEST_HDFS_LIST_PATH} / {@code fabric.test.hdfs.listPath}
 *   <li>{@code FABRIC_TEST_HDFS_WORK_DIR} / {@code fabric.test.hdfs.workDir}
 *   <li>{@code FABRIC_TEST_KRB5_CONF} / {@code fabric.test.krb5.conf}
 * </ul>
 */
public final class TestHdfs {

  private static final String DEFAULT_FS_URI = "hdfs://namenode.example.com:8020";
  private static final String DEFAULT_CLIENT_PRINCIPAL = "svc_hdfs@EXAMPLE.COM";
  private static final String DEFAULT_KEYTAB = "C:/security/keytabs/svc_hdfs.keytab";
  private static final String DEFAULT_KRB5 = "C:/security/krb5.conf";
  private static final String DEFAULT_NN_PRINCIPAL = "hdfs/_HOST@EXAMPLE.COM";
  private static final String DEFAULT_LIST_PATH = "/tmp";
  private static final String DEFAULT_WORK_DIR = "/tmp/fabric-test-basic";

  private TestHdfs() {}

  public static void main(String[] args) throws Exception {
    HdfsConfig cfg = HdfsConfig.load();

    applyKrb5(cfg.krb5Conf());

    Configuration conf = createConfiguration(cfg);
    UserGroupInformation.setConfiguration(conf);
    UserGroupInformation ugi =
        UserGroupInformation.loginUserFromKeytabAndReturnUGI(
            cfg.clientPrincipal(), cfg.keytab().toString());

    System.out.printf(
        "[TestHdfs] Logged in as %s using %s%n", ugi.getUserName(), cfg.keytab().toAbsolutePath());

    ugi.doAs(
        (PrivilegedExceptionAction<Void>)
            () -> {
              try (FileSystem fs = FileSystem.get(new URI(cfg.fsUri()), conf)) {
                listPath(fs, new Path(cfg.listPath()));
                Path created = writeSampleFile(fs, cfg.workDir());
                deleteFile(fs, created);
              }
              return null;
            });
  }

  private static Configuration createConfiguration(HdfsConfig cfg) {
    Configuration conf = new Configuration();
    conf.set("fs.defaultFS", cfg.fsUri());
    conf.set("hadoop.security.authentication", "kerberos");
    conf.set("dfs.namenode.kerberos.principal", cfg.namenodePrincipal());
    conf.setBoolean("dfs.client.use.datanode.hostname", true);
    return conf;
  }

  private static void applyKrb5(java.nio.file.Path krb5Conf) {
    if (krb5Conf != null) {
      System.setProperty("java.security.krb5.conf", krb5Conf.toString());
      System.out.printf("[TestHdfs] Using krb5.conf=%s%n", krb5Conf.toAbsolutePath());
      if (!Files.exists(krb5Conf)) {
        System.err.printf("[TestHdfs] WARNING: krb5.conf not found at %s%n", krb5Conf.toAbsolutePath());
      }
    }
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
  }

  private static void listPath(FileSystem fs, Path target) throws IOException {
    System.out.println("[TestHdfs] Listing " + target);
    if (!fs.exists(target)) {
      System.out.println("[TestHdfs] Path does not exist: " + target);
      return;
    }
    FileStatus[] statuses = fs.listStatus(target);
    if (statuses == null || statuses.length == 0) {
      System.out.println("[TestHdfs] No entries found under " + target);
      return;
    }
    for (FileStatus status : statuses) {
      String kind = status.isDirectory() ? "dir" : status.isFile() ? "file" : "other";
      System.out.printf(
          "  [%s] %s (size=%d owner=%s perms=%s)%n",
          kind, status.getPath(), status.getLen(), status.getOwner(), status.getPermission());
    }
  }

  private static Path writeSampleFile(FileSystem fs, String workDir) throws IOException {
    Path directory = new Path(workDir);
    if (!fs.exists(directory)) {
      fs.mkdirs(directory);
      System.out.println("[TestHdfs] Created directory " + directory);
    }

    Path file = new Path(directory, "fabric-test-" + Instant.now().toEpochMilli() + ".txt");
    String payload = "fabric hdfs smoke test at " + Instant.now();
    try (FSDataOutputStream out = fs.create(file, true)) {
      out.write(payload.getBytes(StandardCharsets.UTF_8));
    }
    System.out.printf("[TestHdfs] Wrote %d bytes to %s%n", payload.length(), file);
    return file;
  }

  private static void deleteFile(FileSystem fs, Path file) throws IOException {
    boolean deleted = fs.delete(file, false);
    System.out.printf("[TestHdfs] Deleted %s: %s%n", file, deleted);
  }

  private record HdfsConfig(
      String fsUri,
      String namenodePrincipal,
      String clientPrincipal,
      java.nio.file.Path keytab,
      java.nio.file.Path krb5Conf,
      String listPath,
      String workDir) {

    static HdfsConfig load() {
      String fsUri = read("FABRIC_TEST_HDFS_URI", "fabric.test.hdfs.uri", DEFAULT_FS_URI);
      String clientPrincipal =
          read(
              "FABRIC_TEST_HDFS_CLIENT_PRINCIPAL",
              "fabric.test.hdfs.clientPrincipal",
              DEFAULT_CLIENT_PRINCIPAL);
      String keytab = read("FABRIC_TEST_HDFS_KEYTAB", "fabric.test.hdfs.keytab", DEFAULT_KEYTAB);
      String nnPrincipal =
          read(
              "FABRIC_TEST_HDFS_NN_PRINCIPAL",
              "fabric.test.hdfs.namenodePrincipal",
              DEFAULT_NN_PRINCIPAL);
      String listPath = read("FABRIC_TEST_HDFS_LIST_PATH", "fabric.test.hdfs.listPath", DEFAULT_LIST_PATH);
      String workDir = read("FABRIC_TEST_HDFS_WORK_DIR", "fabric.test.hdfs.workDir", DEFAULT_WORK_DIR);
      String krb5 = read("FABRIC_TEST_KRB5_CONF", "fabric.test.krb5.conf", DEFAULT_KRB5);

      return new HdfsConfig(
          require(fsUri, "fsUri"),
          require(nnPrincipal, "namenodePrincipal"),
          require(clientPrincipal, "clientPrincipal"),
          requirePath(keytab, "keytab"),
          optionalPath(krb5),
          require(listPath, "listPath"),
          require(workDir, "workDir"));
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
    return Paths.get(value.trim());
  }

  private static java.nio.file.Path optionalPath(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Paths.get(value.trim());
  }
}
