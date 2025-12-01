package org.pronzato.fabric.test.sso;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone HTTPS + OIDC demo harness that mirrors the implementation from fabric-api-ui.
 *
 * <p>All configuration can be overridden via environment variables or system properties:
 *
 * <ul>
 *   <li>SSO_PORT (default 7443)
 *   <li>SSO_KEYSTORE_PATH / SSO_KEYSTORE_PASSWORD
 *   <li>SSO_AUTH_BASE, SSO_AUTH_ENDPOINT, SSO_TOKEN_ENDPOINT
 *   <li>SSO_CLIENT_ID, SSO_CLIENT_SECRET, SSO_REDIRECT_URI
 *   <li>SSO_SCOPE, SSO_USERNAME_CLAIM
 * </ul>
 */
public final class StandaloneHttpsSsoApp {

  private static final Logger LOG = LoggerFactory.getLogger(StandaloneHttpsSsoApp.class);

  private StandaloneHttpsSsoApp() {
  }

  public static void main(String[] args) {
    FabricUiServerOptions options = FabricUiServerOptions.defaults();
    options.httpsEnabled = true;
    options.keyStorePath = requiredSetting("SSO_KEYSTORE_PATH", "/home/foo/foo.jks");
    options.keyStorePassword = requiredSetting("SSO_KEYSTORE_PASSWORD", "foo");
    options.ssoMode = SsoMode.OIDC;
    options.ssoConfig =
        new FabricSsoConfig(
            requiredSetting("SSO_AUTH_BASE", "https://fedssodev.foo.cim"),
            requiredSetting("SSO_AUTH_ENDPOINT", "/as/authorization.oauth2"),
            requiredSetting("SSO_TOKEN_ENDPOINT", "/as/token.oauth2"),
            requiredSetting("SSO_CLIENT_ID", "FOO"),
            requiredSetting("SSO_CLIENT_SECRET", "tohuuygdeetgjkiyfdrth"),
            requiredSetting("SSO_REDIRECT_URI", "https://dev-foo.com"),
            requiredSetting("SSO_SCOPE", "openid"),
            requiredSetting("SSO_USERNAME_CLAIM", "sub"));

    Javalin app =
        FabricUiServerFactory.createServer(options, server -> {
          server.get("/", ctx -> {
            FabricUserIdentity user = ctx.attribute(FabricUiAuthAttributes.FABRIC_USER);
            if (user == null) {
              ctx.json(Map.of(
                  "status", "pending",
                  "message", "No authenticated user in session. Triggering redirect if needed.",
                  "timestamp", Instant.now().toString()));
              return;
            }
            ctx.json(Map.of(
                "status", "ok",
                "username", user.username(),
                "groups", user.groups(),
                "timestamp", Instant.now().toString()));
          });
          server.get("/health", ctx -> ctx.json(Map.of("status", "ok", "time", Instant.now().toString())));
        });

    int port = parsePort(requiredSetting("SSO_PORT", "32001"));
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stopQuietly(app), "standalone-sso-shutdown"));
    app.start(port);
    LOG.info("Standalone HTTPS + SSO app started on https://localhost:{} (or configured host)", port);
  }

  private static void stopQuietly(Javalin app) {
    if (app == null) {
      return;
    }
    try {
      app.stop();
    } catch (Exception ex) {
      LOG.warn("Failed to stop Javalin instance", ex);
    }
  }

  private static String requiredSetting(String key, String fallback) {
    String candidate = firstNonBlank(System.getProperty(key), System.getenv(key));
    return firstNonBlank(candidate, fallback);
  }

  private static String firstNonBlank(String valueOne, String valueTwo) {
    if (valueOne != null && !valueOne.isBlank()) {
      return valueOne.trim();
    }
    if (valueTwo != null && !valueTwo.isBlank()) {
      return valueTwo.trim();
    }
    return null;
  }

  private static int parsePort(String raw) {
    try {
      int parsed = Integer.parseInt(raw);
      return parsed > 0 ? parsed : 7443;
    } catch (Exception ex) {
      return 7443;
    }
  }

  // -------------------------------------------------------------------------
  // Lightweight copies of the reusable HTTPS/SSO helpers from fabric-api-ui
  // -------------------------------------------------------------------------

  private static final class FabricUiServerFactory {
    private FabricUiServerFactory() {
    }

    private static Javalin createServer(
        FabricUiServerOptions opts, Consumer<Javalin> routeConfigurer) {
      Objects.requireNonNull(opts, "opts");
      Objects.requireNonNull(routeConfigurer, "routeConfigurer");
      Javalin app = Javalin.create(config -> {});
      if (opts.httpsEnabled) {
        configureHttps(app, opts);
      }
      FabricUiAuthConfigurer.apply(app, opts);
      routeConfigurer.accept(app);
      return app;
    }

    private static void configureHttps(Javalin app, FabricUiServerOptions opts) {
      if (opts.keyStorePath == null || opts.keyStorePassword == null) {
        throw new IllegalArgumentException("HTTPS requires keystore path and password");
      }
      Path keystorePath = Path.of(opts.keyStorePath);
      if (!Files.exists(keystorePath)) {
        throw new IllegalStateException("Keystore not found at " + keystorePath);
      }
      app.events(event -> event.serverStarting(() -> applyHttpsConnector(app.jettyServer().server(), opts)));
    }

    private static void applyHttpsConnector(Server server, FabricUiServerOptions opts) {
      Connector[] existing = server.getConnectors();
      String host = null;
      int port = 0;
      if (existing.length > 0 && existing[0] instanceof ServerConnector connector) {
        host = connector.getHost();
        port = connector.getPort();
      }
      Arrays.stream(existing).forEach(connector -> {
        try {
          connector.stop();
        } catch (Exception ignored) {
        }
        server.removeConnector(connector);
      });

      HttpConfiguration httpsConfig = new HttpConfiguration();
      httpsConfig.setSecureScheme("https");
      if (port > 0) {
        httpsConfig.setSecurePort(port);
      }
      httpsConfig.addCustomizer(new SecureRequestCustomizer());

      SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
      sslContextFactory.setKeyStorePath(opts.keyStorePath);
      sslContextFactory.setKeyStorePassword(opts.keyStorePassword);
      sslContextFactory.setKeyManagerPassword(opts.keyStorePassword);

      ServerConnector sslConnector = new ServerConnector(
          server,
          new SslConnectionFactory(sslContextFactory, "http/1.1"),
          new HttpConnectionFactory(httpsConfig));
      if (host != null && !host.isBlank()) {
        sslConnector.setHost(host);
      }
      if (port > 0) {
        sslConnector.setPort(port);
      }
      server.addConnector(sslConnector);
    }
  }

  private static final class FabricUiAuthConfigurer {
    private FabricUiAuthConfigurer() {
    }

    private static void apply(Javalin app, FabricUiServerOptions opts) {
      SsoMode mode = opts.ssoMode == null ? SsoMode.NONE : opts.ssoMode;
      IdentityProvider identityProvider = IdentityProviderFactory.forOptions(opts);
      app.before(ctx -> {
        FabricUserIdentity user = identityProvider.resolve(ctx);
        if (mode == SsoMode.OIDC
            && user == null
            && shouldRedirectToLogin(ctx)) {
          LOG.debug("Unauthenticated request detected, redirecting to /login");
          ctx.redirect("/login");
          ctx.skipRemainingHandlers();
          return;
        }
        ctx.attribute(FabricUiAuthAttributes.FABRIC_USER, user);
      });

      if (mode == SsoMode.OIDC) {
        FabricSsoConfig config =
            Objects.requireNonNull(opts.ssoConfig, "ssoConfig required when SSO mode is OIDC");
        new FabricOidcSsoHandler(config).registerRoutes(app);
      }
    }

    private static boolean shouldRedirectToLogin(Context ctx) {
      String path = ctx.path();
      HandlerType type = ctx.handlerType();
      if (type != HandlerType.GET) {
        return false;
      }
      return !"/login".equals(path) && !"/oidc/callback".equals(path);
    }
  }

  private interface IdentityProvider {
    FabricUserIdentity resolve(Context ctx);
  }

  private static final class NoneIdentityProvider implements IdentityProvider {
    @Override
    public FabricUserIdentity resolve(Context ctx) {
      return null;
    }
  }

  private static final class OidcSessionIdentityProvider implements IdentityProvider {
    @Override
    public FabricUserIdentity resolve(Context ctx) {
      return ctx.sessionAttribute(FabricUiAuthAttributes.FABRIC_USER);
    }
  }

  private static final class DevSpoofIdentityProvider implements IdentityProvider {
    private final SpoofConfig config;

    private DevSpoofIdentityProvider(SpoofConfig config) {
      this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public FabricUserIdentity resolve(Context ctx) {
      // TODO ensure spoof mode is never enabled in production deployments.
      String username =
          firstNonBlank(ctx.queryParam("asUser"), ctx.header("X-Dev-User"));
      if (username == null || username.isBlank()) {
        username = config.defaultUsername();
      }
      String groupOverride = firstNonBlank(ctx.queryParam("asGroups"), ctx.header("X-Dev-Groups"));
      List<String> groups =
          groupOverride == null
              ? config.defaultGroups()
              : Arrays.stream(groupOverride.split("[,;]"))
                  .map(value -> value == null ? "" : value.trim())
                  .filter(value -> !value.isEmpty())
                  .toList();
      if (groups.isEmpty()) {
        groups = config.defaultGroups();
      }
      return new FabricUserIdentity(username, groups);
    }
  }

  private static final class IdentityProviderFactory {
    private IdentityProviderFactory() {
    }

    private static IdentityProvider forOptions(FabricUiServerOptions opts) {
      SsoMode mode = opts.ssoMode == null ? SsoMode.NONE : opts.ssoMode;
      return switch (mode) {
        case NONE -> new NoneIdentityProvider();
        case OIDC -> new OidcSessionIdentityProvider();
        case SPOOF -> new DevSpoofIdentityProvider(
            Objects.requireNonNull(opts.spoofConfig, "spoofConfig required for spoof mode"));
      };
    }
  }

  private static final class FabricOidcSsoHandler {
    private final FabricSsoConfig config;

    private FabricOidcSsoHandler(FabricSsoConfig config) {
      this.config = config;
    }

    private void registerRoutes(Javalin app) {
      app.get("/login", this::handleLogin);
      app.get("/oidc/callback", this::handleCallback);
    }

    private void handleLogin(Context ctx) {
      try {
        State state = new State(UUID.randomUUID().toString());
        Nonce nonce = new Nonce(UUID.randomUUID().toString());
        ctx.sessionAttribute(FabricUiAuthAttributes.OIDC_STATE, state.getValue());
        ctx.sessionAttribute(FabricUiAuthAttributes.OIDC_NONCE, nonce.getValue());
        ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
        Scope scope = Scope.parse(config.scope());
        ClientID clientId = new ClientID(config.clientId());
        var authRequest =
            new com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder(
                responseType, scope, clientId, URI.create(config.redirectUri()))
                .endpointURI(URI.create(config.getAuthUrl()))
                .state(state)
                .nonce(nonce)
                .build();
        ctx.redirect(authRequest.toURI().toString());
      } catch (Exception ex) {
        LOG.error("Failed to initiate OIDC login flow", ex);
        ctx.status(500).result("Unable to start login");
      }
    }

    private void handleCallback(Context ctx) {
      String expectedState = ctx.sessionAttribute(FabricUiAuthAttributes.OIDC_STATE);
      ctx.sessionAttribute(FabricUiAuthAttributes.OIDC_STATE, null);
      String providedState = ctx.queryParam("state");
      if (expectedState == null || providedState == null || !expectedState.equals(providedState)) {
        ctx.status(400).result("Invalid authentication state");
        return;
      }
      String code = ctx.queryParam("code");
      if (code == null || code.isBlank()) {
        ctx.status(400).result("Missing authorization code");
        return;
      }
      try {
        OIDCTokens tokens = exchangeForTokens(code.trim());
        FabricUserIdentity identity = buildIdentity(ctx, tokens);
        ctx.sessionAttribute(FabricUiAuthAttributes.FABRIC_USER, identity);
        ctx.redirect("/");
      } catch (Exception ex) {
        LOG.warn("OIDC callback failed", ex);
        ctx.status(500).result("Authentication failed");
      }
    }

    private OIDCTokens exchangeForTokens(String codeValue) throws Exception {
      AuthorizationCode code = new AuthorizationCode(codeValue);
      AuthorizationCodeGrant grant =
          new AuthorizationCodeGrant(code, URI.create(config.redirectUri()));
      ClientAuthentication clientAuth =
          new ClientSecretBasic(new ClientID(config.clientId()), new Secret(config.clientSecret()));
      TokenRequest request = new TokenRequest(URI.create(config.getTokenUrl()), clientAuth, grant);
      HTTPResponse response = request.toHTTPRequest().send();
      TokenResponse parsed = OIDCTokenResponseParser.parse(response);
      if (!parsed.indicatesSuccess()) {
        ErrorObject error = parsed.toErrorResponse().getErrorObject();
        throw new IllegalStateException("Token exchange failed: " + error);
      }
      return ((OIDCTokenResponse) parsed).getOIDCTokens();
    }

    private FabricUserIdentity buildIdentity(Context ctx, OIDCTokens tokens) throws Exception {
      if (tokens.getIDToken() == null) {
        throw new IllegalStateException("Missing ID token in OIDC response");
      }
      JWTClaimsSet claims = tokens.getIDToken().getJWTClaimsSet();
      verifyNonce(ctx, claims.getStringClaim("nonce"));
      String username = claims.getStringClaim(config.usernameClaim());
      if (username == null || username.isBlank()) {
        throw new IllegalStateException(
            "OIDC response missing username claim: " + config.usernameClaim());
      }
      List<String> groups = extractGroups(claims);
      return new FabricUserIdentity(username, groups);
    }

    private void verifyNonce(Context ctx, String tokenNonce) {
      String expectedNonce = ctx.sessionAttribute(FabricUiAuthAttributes.OIDC_NONCE);
      ctx.sessionAttribute(FabricUiAuthAttributes.OIDC_NONCE, null);
      if (expectedNonce == null || tokenNonce == null || !expectedNonce.equals(tokenNonce)) {
        throw new IllegalStateException("Nonce validation failed");
      }
    }

    private List<String> extractGroups(JWTClaimsSet claims) {
      Object claim = claims.getClaim("groups");
      if (claim instanceof List<?> rawList) {
        List<String> normalized = new ArrayList<>();
        for (Object value : rawList) {
          if (value instanceof String text && !text.isBlank()) {
            normalized.add(text.trim());
          }
        }
        if (!normalized.isEmpty()) {
          return normalized;
        }
      } else if (claim instanceof String text && !text.isBlank()) {
        return List.of(text.trim());
      }
      return List.of();
    }
  }

  private static final class FabricUiAuthAttributes {
    private static final String FABRIC_USER = "fabric.user";
    private static final String OIDC_STATE = "fabric.oidc.state";
    private static final String OIDC_NONCE = "fabric.oidc.nonce";

    private FabricUiAuthAttributes() {
    }
  }

  private enum SsoMode {
    NONE,
    OIDC,
    SPOOF
  }

  private static final class FabricUserIdentity {
    private final String username;
    private final List<String> groups;

    private FabricUserIdentity(String username, List<String> groups) {
      this.username = Objects.requireNonNull(username, "username");
      this.groups = groups == null ? List.of() : List.copyOf(groups);
    }

    private String username() {
      return username;
    }

    private List<String> groups() {
      return groups;
    }
  }

  private static final class FabricSsoConfig {
    private final String authServerBase;
    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    private final String usernameClaim;

    private FabricSsoConfig(
        String authServerBase,
        String authorizationEndpoint,
        String tokenEndpoint,
        String clientId,
        String clientSecret,
        String redirectUri,
        String scope,
        String usernameClaim) {
      this.authServerBase = authServerBase;
      this.authorizationEndpoint = authorizationEndpoint;
      this.tokenEndpoint = tokenEndpoint;
      this.clientId = clientId;
      this.clientSecret = clientSecret;
      this.redirectUri = redirectUri;
      this.scope = scope;
      this.usernameClaim = usernameClaim;
    }

    private String clientId() {
      return clientId;
    }

    private String clientSecret() {
      return clientSecret;
    }

    private String redirectUri() {
      return redirectUri;
    }

    private String scope() {
      return scope;
    }

    private String usernameClaim() {
      return usernameClaim;
    }

    private String getAuthUrl() {
      return join(authServerBase, authorizationEndpoint);
    }

    private String getTokenUrl() {
      return join(authServerBase, tokenEndpoint);
    }

    private static String join(String base, String endpoint) {
      if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
        return endpoint;
      }
      if (base.endsWith("/") && endpoint.startsWith("/")) {
        return base + endpoint.substring(1);
      }
      if (!base.endsWith("/") && !endpoint.startsWith("/")) {
        return base + "/" + endpoint;
      }
      return base + endpoint;
    }
  }

  private static final class SpoofConfig {
    private final String defaultUsername;
    private final List<String> defaultGroups;

    private SpoofConfig(String defaultUsername, List<String> defaultGroups) {
      this.defaultUsername = defaultUsername;
      this.defaultGroups = defaultGroups == null ? List.of() : List.copyOf(defaultGroups);
    }

    private String defaultUsername() {
      return defaultUsername;
    }

    private List<String> defaultGroups() {
      return defaultGroups;
    }
  }

  private static final class FabricUiServerOptions {
    private boolean httpsEnabled;
    private String keyStorePath;
    private String keyStorePassword;
    private SsoMode ssoMode;
    private FabricSsoConfig ssoConfig;
    private SpoofConfig spoofConfig;

    private FabricUiServerOptions() {
    }

    private static FabricUiServerOptions defaults() {
      FabricUiServerOptions options = new FabricUiServerOptions();
      options.httpsEnabled = false;
      options.ssoMode = SsoMode.NONE;
      return options;
    }
  }
}
