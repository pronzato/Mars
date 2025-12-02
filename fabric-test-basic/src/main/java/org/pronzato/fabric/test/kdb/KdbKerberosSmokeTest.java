package org.pronzato.fabric.test.kdb;

import org.pronzato.fabric.api.kerberos.KerberosApi;
import org.pronzato.fabric.test.TestSecurityConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

/** Standalone KDB Kerberos (SPNEGO) connectivity smoke test. */
public final class KdbKerberosSmokeTest {

  private static final String HOST = "kdb-demo-host";
  private static final int PORT = 5011;
  private static final String QUERY = ".z.p";
  private static final String SERVICE_PRINCIPAL = TestSecurityConfig.KDB_SERVICE_PRINCIPAL;
  private static final String CLIENT_PRINCIPAL = TestSecurityConfig.KDB_CLIENT_PRINCIPAL;
  private static final Path KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path KRB5 = TestSecurityConfig.KRB5_CONF;

  private KdbKerberosSmokeTest() {}

  public static void main(String[] args) {
    System.out.println("[KdbKerberos] Starting Kerberos-enabled smoke test for " + HOST + ":" + PORT);
    if (!Files.exists(KEYTAB)) {
      System.err.println("[KdbKerberos] Keytab missing: " + KEYTAB.toAbsolutePath());
      return;
    }
    KerberosApi kerberos = null;
    try {
      kerberos = startKerberos();
      Object client = kerberos.doAs(KdbKerberosSmokeTest::openClient);
      try {
        Object result = kerberos.doAs(() -> execute(client, QUERY));
        System.out.println("[KdbKerberos] Result (" + QUERY + "): " + render(result));
      } finally {
        closeQuietly(client);
      }
      System.out.println("KDB Kerberos smoke test SUCCESS");
    } catch (Exception ex) {
      System.err.println("KDB Kerberos smoke test FAILURE");
      ex.printStackTrace(System.err);
    } finally {
      closeQuietly(kerberos);
    }
  }

  private static KerberosApi startKerberos() {
    try {
      KerberosApi.Builder builder =
          KerberosApi.builder()
              .withPrincipal(CLIENT_PRINCIPAL)
              .withKeytabPath(KEYTAB)
              .withReloginEvery(Duration.ZERO);
      if (Files.exists(KRB5)) {
        builder.withKrb5Path(KRB5);
      }
      KerberosApi api = builder.build().start();
      System.out.println(
          "[KdbKerberos] Logged in principal="
              + api.principal()
              + " krb5="
              + api.krb5Path().toAbsolutePath());
      return api;
    } catch (Exception ex) {
      throw new IllegalStateException("Kerberos bootstrap failed: " + ex.getMessage(), ex);
    }
  }

  private static Object openClient() {
    try {
      Class<?> clientClass = Class.forName("kx.c");
      Constructor<?> ctor = clientClass.getConstructor(String.class, int.class);
      Object client = ctor.newInstance(HOST, PORT);
      applyIfPresent(clientClass, client, "principal", SERVICE_PRINCIPAL);
      applyIfPresent(clientClass, client, "servicePrincipal", SERVICE_PRINCIPAL);
      System.out.println(
          "[KdbKerberos] Opened Kerberos KDB connection to "
              + HOST
              + ":"
              + PORT
              + " (service SPN "
              + SERVICE_PRINCIPAL
              + ")");
      return client;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to open Kerberos KDB client", ex);
    }
  }

  private static void applyIfPresent(Class<?> clientClass, Object instance, String method, String value) {
    try {
      Method setter = clientClass.getMethod(method, String.class);
      setter.invoke(instance, value);
      System.out.println("[KdbKerberos] Applied " + method + "=" + value);
    } catch (NoSuchMethodException ignored) {
      // method not present on this client build
    } catch (InvocationTargetException | IllegalAccessException ex) {
      System.err.println("[KdbKerberos] Unable to call " + method + ": " + ex.getMessage());
    }
  }

  private static Object execute(Object client, String query) {
    try {
      Method kMethod = client.getClass().getMethod("k", Object.class);
    try {
      return kMethod.invoke(client, query);
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw ex;
    }
    } catch (Exception ex) {
      throw new IllegalStateException("KDB query failed for '" + query + "'", ex);
    }
  }

  private static String render(Object value) {
    if (value == null) {
      return "<null>";
    }
    if (value.getClass().isArray()) {
      if (value instanceof boolean[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof byte[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof short[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof int[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof long[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof float[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof double[] arr) {
        return Arrays.toString(arr);
      }
      if (value instanceof Object[] arr) {
        return Arrays.toString(arr);
      }
    }
    return String.valueOf(value);
  }

  private static void closeQuietly(Object closeable) {
    if (closeable == null) {
      return;
    }
    try {
      if (closeable instanceof AutoCloseable autoCloseable) {
        autoCloseable.close();
        return;
      }
      Method closeMethod;
      try {
        closeMethod = closeable.getClass().getMethod("Close");
      } catch (NoSuchMethodException ex) {
        closeMethod = closeable.getClass().getMethod("close");
      }
      closeMethod.invoke(closeable);
    } catch (Exception ignore) {
      // best-effort cleanup
    }
  }
}
