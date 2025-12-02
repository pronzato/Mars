package org.pronzato.fabric.test.http;

import org.pronzato.fabric.api.kerberos.KerberosApi;
import org.pronzato.fabric.test.TestSecurityConfig;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.KerberosCredentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/** Minimal HTTPS + Kerberos (SPNEGO) smoke test. */
public final class HttpKerberosSmokeTest {

  private static final String TARGET_SERVICE_PRINCIPAL = TestSecurityConfig.HTTP_SERVICE_PRINCIPAL;
  private static final String CLIENT_PRINCIPAL = TestSecurityConfig.HTTP_CLIENT_PRINCIPAL;
  private static final Path KEYTAB = TestSecurityConfig.KEYTAB;
  private static final Path KRB5 = TestSecurityConfig.KRB5_CONF;

  private static final String TLS_CERT = TestSecurityConfig.TLS_CERT_RESOURCE;
  private static final String TLS_KEY = TestSecurityConfig.TLS_KEY_RESOURCE;
  private static final String TLS_TRUST = TestSecurityConfig.TLS_TRUST_RESOURCE;

  private static final String TARGET_HOST = extractHost(TARGET_SERVICE_PRINCIPAL);
  private static final String URL = "https://" + TARGET_HOST + ":8443/health";
  private static final int PREVIEW_LIMIT = 400;

  private HttpKerberosSmokeTest() {}

  public static void main(String[] args) {
    System.out.println("[HttpKerberos] Starting HTTP Kerberos smoke test for " + URL);
    if (!Files.exists(KEYTAB)) {
      System.err.println("[HttpKerberos] Keytab missing: " + KEYTAB.toAbsolutePath());
      return;
    }
    try (KerberosApi kerberos = startKerberos()) {
      SSLContext sslContext = buildSslContext();
      GSSCredential gssCredential = kerberos.doAs(HttpKerberosSmokeTest::createGssCredential);
      try (CloseableHttpClient client = buildHttpClient(sslContext, gssCredential)) {
        executeRequest(client, kerberos);
      }
      System.out.println("HTTP Kerberos smoke test SUCCESS");
    } catch (Exception ex) {
      System.err.println("HTTP Kerberos smoke test FAILURE");
      ex.printStackTrace(System.err);
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
          "[HttpKerberos] Logged in as " + api.principal() + " using keytab " + api.keytabPath());
      return api;
    } catch (Exception ex) {
      throw new IllegalStateException("Kerberos bootstrap failed: " + ex.getMessage(), ex);
    }
  }

  private static CloseableHttpClient buildHttpClient(SSLContext sslContext, GSSCredential gssCredential) {
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new KerberosCredentials(gssCredential));

    Registry<AuthSchemeProvider> authSchemes =
        RegistryBuilder.<AuthSchemeProvider>create()
            .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true))
            .build();

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setTargetPreferredAuthSchemes(List.of(AuthSchemes.SPNEGO))
            .build();

    SSLConnectionSocketFactory sslSocketFactory =
        new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

    return HttpClients.custom()
        .setDefaultAuthSchemeRegistry(authSchemes)
        .setDefaultCredentialsProvider(credentialsProvider)
        .setDefaultRequestConfig(requestConfig)
        .setSSLSocketFactory(sslSocketFactory)
        .build();
  }

  private static void executeRequest(CloseableHttpClient client, KerberosApi kerberos)
      throws IOException {
    HttpGet request = new HttpGet(URL);
    request.setHeader(HttpHeaders.CONNECTION, "close");
    request.setHeader(HttpHeaders.AUTHORIZATION, kerberos.buildHttpNegotiateHeader(TARGET_HOST));

    System.out.println("[HttpKerberos] Sending GET " + URL);
    try (CloseableHttpResponse response = client.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body =
          response.getEntity() == null
              ? ""
              : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      System.out.println("[HttpKerberos] HTTP status " + statusCode);
      System.out.println("[HttpKerberos] Body preview:");
      System.out.println(body.length() <= PREVIEW_LIMIT ? body : body.substring(0, PREVIEW_LIMIT));
      Header[] negotiate = response.getHeaders("WWW-Authenticate");
      if (negotiate != null && negotiate.length > 0) {
        System.out.println("[HttpKerberos] Server authentication headers:");
        for (Header header : negotiate) {
          System.out.println("  " + header.getName() + ": " + header.getValue());
        }
      }
    }
  }

  private static SSLContext buildSslContext() throws Exception {
    X509Certificate clientCert = loadCertificate(TLS_CERT);
    PrivateKey privateKey = loadPrivateKey(TLS_KEY);
    X509Certificate trustCert = loadCertificate(TLS_TRUST);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(buildKeyStore(clientCert, privateKey), new char[0]);

    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(buildTrustStore(trustCert));

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return sslContext;
  }

  private static java.security.KeyStore buildKeyStore(Certificate cert, PrivateKey key)
      throws Exception {
    java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    keyStore.setKeyEntry("client", key, new char[0], new Certificate[] {cert});
    return keyStore;
  }

  private static java.security.KeyStore buildTrustStore(Certificate trustCert) throws Exception {
    java.security.KeyStore trustStore = java.security.KeyStore.getInstance("PKCS12");
    trustStore.load(null, null);
    trustStore.setCertificateEntry("ca", trustCert);
    return trustStore;
  }

  private static X509Certificate loadCertificate(String resourceName) throws Exception {
    try (InputStream in = resourceStream(resourceName)) {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) factory.generateCertificate(in);
    }
  }

  private static PrivateKey loadPrivateKey(String resourceName) throws Exception {
    String pem = new String(resourceBytes(resourceName), StandardCharsets.US_ASCII);
    String normalized =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
    byte[] keyBytes = Base64.getDecoder().decode(normalized);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    try {
      return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    } catch (Exception ex) {
      return KeyFactory.getInstance("EC").generatePrivate(keySpec);
    }
  }

  private static InputStream resourceStream(String name) {
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    if (in == null) {
      throw new IllegalArgumentException("Missing classpath resource: " + name);
    }
    return in;
  }

  private static byte[] resourceBytes(String name) throws IOException {
    try (InputStream in = resourceStream(name)) {
      return in.readAllBytes();
    }
  }

  private static GSSCredential createGssCredential() {
    try {
      return GSSManager.getInstance()
          .createCredential(
              null,
              GSSCredential.DEFAULT_LIFETIME,
              new Oid("1.3.6.1.5.5.2"),
              GSSCredential.INITIATE_ONLY);
    } catch (GSSException e) {
      throw new IllegalStateException("Unable to create SPNEGO credential", e);
    }
  }

  private static String extractHost(String spn) {
    if (spn == null) {
      return "localhost";
    }
    int slash = spn.indexOf('/');
    int at = spn.indexOf('@');
    if (slash >= 0 && at > slash + 1) {
      return spn.substring(slash + 1, at);
    }
    return spn;
  }
}
