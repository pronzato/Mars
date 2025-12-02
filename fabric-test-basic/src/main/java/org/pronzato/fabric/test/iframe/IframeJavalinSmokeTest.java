package org.pronzato.fabric.test.iframe;

import io.javalin.Javalin;

/** Minimal Javalin server that serves an iframe test page. */
public final class IframeJavalinSmokeTest {

  private static final int PORT = 7070;
  private static final String IFRAME_URL = "https://www.example.com/";

  private IframeJavalinSmokeTest() {}

  public static void main(String[] args) throws Exception {
    Javalin app = Javalin.create(config -> {});

    app.get(
        "/",
        ctx -> {
          ctx.header("X-Frame-Options", "ALLOWALL");
          ctx.header("Content-Security-Policy", "frame-ancestors *");
          ctx.html(indexHtml());
        });

    app.get(
        "/iframe-content",
        ctx -> {
          ctx.header("X-Frame-Options", "ALLOWALL");
          ctx.header("Content-Security-Policy", "frame-ancestors *");
          String inner =
              """
              <!DOCTYPE html>
              <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <title>Iframe Inner Content</title>
                <style>
                  body { font-family: Arial, sans-serif; margin: 32px; color: #0f172a; }
                  .card { padding: 18px; border: 1px solid #cbd5e1; border-radius: 10px; background: #f8fafc; }
                </style>
              </head>
              <body>
                <div class="card">
                  <h1>Iframe Content</h1>
                  <p>This page is intentionally simple to test same-origin iframe rendering.</p>
                  <p>Timestamp: %s</p>
                </div>
              </body>
              </html>
              """
                  .formatted(java.time.Instant.now());
          ctx.html(inner);
        });

    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> stopQuietly(app), "iframe-javalin-shutdown"));

    app.start(PORT);
    System.out.println("[Iframe] Started on http://localhost:" + PORT + "/");
    System.out.println("[Iframe] External iframe target: " + IFRAME_URL);
    app.jettyServer().server().join();
  }

  private static String indexHtml() {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <title>Fabric Iframe Smoke Test</title>
          <style>
            :root { color-scheme: light; }
            body { font-family: Arial, sans-serif; margin: 0; padding: 0; background: #f1f5f9; color: #0f172a; }
            header { padding: 18px 24px; background: linear-gradient(90deg, #0f172a, #1e293b); color: #e2e8f0; }
            main { padding: 24px; max-width: 980px; margin: 0 auto; }
            .panel { background: #fff; border: 1px solid #cbd5e1; border-radius: 12px; padding: 18px 20px; box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06); }
            iframe { width: 100%; height: 540px; border: 1px solid #cbd5e1; border-radius: 10px; background: #fff; }
            ul { margin-top: 8px; }
          </style>
        </head>
        <body>
          <header>
            <h2>Fabric Iframe Smoke Test</h2>
            <p>Use this page to verify whether corporate browsers or proxies block iframe content.</p>
          </header>
          <main>
            <div class="panel">
              <p>External iframe target: <strong>%s</strong></p>
              <iframe src="%s" title="External iframe"></iframe>
            </div>
            <div class="panel" style="margin-top: 18px;">
              <p>Same-origin iframe content served from <code>/iframe-content</code>:</p>
              <iframe src="/iframe-content" title="Local iframe"></iframe>
            </div>
          </main>
        </body>
        </html>
        """
        .formatted(IFRAME_URL, IFRAME_URL);
  }

  private static void stopQuietly(Javalin app) {
    if (app == null) {
      return;
    }
    try {
      app.stop();
    } catch (Exception ignored) {
      // best effort
    }
  }
}
