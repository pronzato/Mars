package org.pronzato.fabric.test.kdb;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

/** Standalone KDB username/password connectivity smoke test. */
public final class KdbBasicAuthSmokeTest {

  private static final String HOST = "kdb-demo-host";
  private static final int PORT = 5010;
  private static final String USERNAME = "demo";
  private static final String PASSWORD = "demo";
  private static final String QUERY_ONE = ".z.p";
  private static final String QUERY_TWO = "1+1";

  private KdbBasicAuthSmokeTest() {}

  public static void main(String[] args) {
    System.out.println(
        "[KdbBasic] Starting basic auth smoke test against " + HOST + ":" + PORT);
    try {
      Object client = openClient();
      try {
        Object now = execute(client, QUERY_ONE);
        System.out.println("[KdbBasic] Result (" + QUERY_ONE + "): " + render(now));

        Object arithmetic = execute(client, QUERY_TWO);
        System.out.println("[KdbBasic] Result (" + QUERY_TWO + "): " + render(arithmetic));
      } finally {
        closeQuietly(client);
      }
      System.out.println("KDB basic auth smoke test SUCCESS");
    } catch (Exception ex) {
      System.err.println("KDB basic auth smoke test FAILURE");
      ex.printStackTrace(System.err);
    }
  }

  private static Object openClient() throws Exception {
    Class<?> clientClass = Class.forName("kx.c");
    Constructor<?> ctorWithAuth = null;
    try {
      ctorWithAuth = clientClass.getConstructor(String.class, int.class, String.class);
    } catch (NoSuchMethodException ignore) {
      // optional in some client versions
    }
    Constructor<?> ctorNoAuth = clientClass.getConstructor(String.class, int.class);
    if (ctorWithAuth != null) {
      String credential = PASSWORD == null || PASSWORD.isBlank() ? USERNAME : USERNAME + ":" + PASSWORD;
      Object client = ctorWithAuth.newInstance(HOST, PORT, credential);
      System.out.println(
          "[KdbBasic] Connected with credentials to " + HOST + ":" + PORT + " as " + USERNAME);
      return client;
    }
    Object client = ctorNoAuth.newInstance(HOST, PORT);
    System.out.println("[KdbBasic] Connected without credentials to " + HOST + ":" + PORT);
    return client;
  }

  private static Object execute(Object client, String query) throws Exception {
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
    if (value instanceof Instant instant) {
      return instant.toString();
    }
    return String.valueOf(value);
  }

  private static void closeQuietly(Object client) {
    if (client == null) {
      return;
    }
    try {
      Method closeMethod;
      try {
        closeMethod = client.getClass().getMethod("Close");
      } catch (NoSuchMethodException ex) {
        closeMethod = client.getClass().getMethod("close");
      }
      closeMethod.invoke(client);
    } catch (Exception ignore) {
      // best-effort cleanup
    }
  }
}
