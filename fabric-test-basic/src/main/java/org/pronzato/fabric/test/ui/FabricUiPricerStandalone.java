package org.pronzato.fabric.test.ui;

import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Standalone copy of the Fabric UI pricer demo without module dependencies. */
public final class FabricUiPricerStandalone {

  private static final int PORT = 8085;
  private static final long TICK_MILLIS = 300L;
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

  private static final List<String> SYMBOLS = List.of("AAPL", "MSFT", "GOOG", "AMZN", "TSLA");
  private static final Map<String, Price> PRICES = new ConcurrentHashMap<>();
  private static final ScheduledExecutorService FEED =
      Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "fabric-ui-pricer-feed");
        t.setDaemon(true);
        return t;
      });
  private static final Random RANDOM = new Random();

  private FabricUiPricerStandalone() {}

  public static void main(String[] args) {
    initializePrices();

    Set<WsContext> sessions = new CopyOnWriteArraySet<>();
    Javalin app = Javalin.create(config -> {});

    app.get("/", ctx -> ctx.html(indexHtml()));
    app.ws("/ws/prices", ws -> registerWebSocketHandlers(ws, sessions));

    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> {
          stopFeed();
          stopQuietly(app);
        }, "fabric-ui-pricer-shutdown"));

    startFeed(sessions);

    app.start(PORT);
    System.out.println(
        "FabricUiPricerStandalone started on http://localhost:" + PORT + " (ws endpoint /ws/prices)");
  }

  private static void registerWebSocketHandlers(WsConfig ws, Set<WsContext> sessions) {
    ws.onConnect(ctx -> {
      sessions.add(ctx);
      sendSnapshot(ctx);
    });
    ws.onClose(sessions::remove);
    ws.onError(sessions::remove);
  }

  private static void startFeed(Set<WsContext> sessions) {
    FEED.scheduleAtFixedRate(() -> {
      try {
        applyRandomChanges();
        broadcastSnapshot(sessions);
      } catch (Exception ex) {
        System.err.println("[Pricer] Feed tick failed: " + ex.getMessage());
      }
    }, 0L, TICK_MILLIS, TimeUnit.MILLISECONDS);
  }

  private static void stopFeed() {
    FEED.shutdownNow();
  }

  private static void initializePrices() {
    Map<String, Double> basePrices =
        Map.of("AAPL", 190.12, "MSFT", 420.41, "GOOG", 175.33, "AMZN", 180.88, "TSLA", 205.55);
    basePrices.forEach((symbol, basePrice) -> PRICES.put(symbol, new Price(symbol, basePrice)));
  }

  private static void applyRandomChanges() {
    SYMBOLS.stream()
        .map(PRICES::get)
        .forEach(
            price -> {
              if (price != null) {
                double delta = (RANDOM.nextDouble() - 0.5) * 0.6; // +/- $0.30
                price.update(delta);
              }
            });
  }

  private static void sendSnapshot(WsConnectContext ctx) {
    try {
      ctx.send(snapshotJson());
    } catch (Exception ex) {
      System.err.println("[Pricer] Failed to send initial snapshot: " + ex.getMessage());
    }
  }

  private static void broadcastSnapshot(Set<WsContext> sessions) {
    String payload = snapshotJson();
    sessions.forEach(
        session -> {
          try {
            session.send(payload);
          } catch (Exception ex) {
            sessions.remove(session);
          }
        });
  }

  private static String snapshotJson() {
    List<PriceView> views = new ArrayList<>(SYMBOLS.size());
    SYMBOLS.forEach(
        symbol -> {
          Price price = PRICES.get(symbol);
          if (price != null) {
            views.add(price.view());
          }
        });
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < views.size(); i++) {
      PriceView view = views.get(i);
      if (i > 0) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"symbol\":\"").append(view.symbol()).append("\",");
      sb.append("\"lastPrice\":").append(format(view.lastPrice())).append(",");
      sb.append("\"bidPrice\":").append(format(view.bidPrice())).append(",");
      sb.append("\"askPrice\":").append(format(view.askPrice())).append(",");
      sb.append("\"lastUpdateTime\":\"").append(view.lastUpdateTime()).append("\"");
      sb.append("}");
    }
    sb.append("]");
    return sb.toString();
  }

  private static String format(double value) {
    return String.format(Locale.US, "%.2f", value);
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

  private static String indexHtml() {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <title>Fabric UI Pricer Standalone</title>
          <style>
            :root {
              color-scheme: dark;
              --panel: #111828;
              --panel-border: #1e2435;
              --panel-highlight: #1f2c44;
              --text-muted: #8d9ab5;
              --accent: #36d1dc;
              --accent-strong: #ff3860;
              --glow: #3f6ffc;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              font-family: 'Segoe UI', 'Roboto', sans-serif;
              background: radial-gradient(circle at 20% 20%, rgba(70,104,255,0.4), transparent 50%), #05070f;
              min-height: 100vh;
              color: #f4f7ff;
            }
            .terminal-chrome {
              padding: 18px 32px 12px;
              background: linear-gradient(90deg, #11152a, #0a0d1c);
              border-bottom: 1px solid #1e2435;
              text-transform: uppercase;
              letter-spacing: 0.08em;
              font-size: 12px;
            }
            .chrome-bar {
              display: flex;
              justify-content: space-between;
              font-weight: 600;
              color: #9cb5ff;
            }
            .chrome-env {
              color: var(--accent);
            }
            .chrome-sub {
              margin-top: 6px;
              display: flex;
              gap: 18px;
              color: var(--text-muted);
            }
            main {
              padding: 36px;
            }
            .grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
              gap: 24px;
            }
            .quote-card {
              background: var(--panel);
              border: 1px solid var(--panel-border);
              border-radius: 18px;
              padding: 20px;
              box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
              display: flex;
              flex-direction: column;
              gap: 18px;
              transition: transform 0.2s ease, border-color 0.2s ease;
            }
            .quote-card:hover {
              transform: translateY(-4px);
              border-color: #3c4b73;
            }
            .quote-header {
              display: flex;
              justify-content: space-between;
              align-items: flex-start;
            }
            .ticker {
              font-size: 32px;
              font-weight: 700;
              letter-spacing: 0.06em;
            }
            .quote-header .label {
              color: var(--text-muted);
              font-size: 12px;
              margin-top: 6px;
              text-transform: uppercase;
            }
            .last-block {
              text-align: right;
            }
            .last-label {
              font-size: 11px;
              color: var(--text-muted);
              letter-spacing: 0.08em;
            }
            .last-value {
              font-size: 26px;
              font-weight: 600;
            }
            .price-stage {
              background: radial-gradient(circle at 30% 40%, rgba(63,111,252,0.4), transparent 70%), var(--panel-highlight);
              border-radius: 16px;
              padding: 18px;
              border: 1px solid rgba(255,255,255,0.04);
              position: relative;
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: 16px;
            }
            .price-glow {
              grid-column: span 2;
              text-align: center;
              font-size: 42px;
              font-weight: 700;
              color: #fff;
              text-shadow: 0 0 20px rgba(63,111,252,0.8);
            }
            .side {
              background: rgba(0,0,0,0.2);
              border-radius: 12px;
              padding: 12px;
              font-size: 14px;
              display: flex;
              justify-content: space-between;
              align-items: center;
              border: 1px solid rgba(255,255,255,0.05);
            }
            .side strong {
              font-size: 20px;
            }
            .side-bid strong { color: #47f8c2; }
            .side-ask strong { color: #ff7fbf; }
            .meta {
              display: flex;
              justify-content: space-between;
              align-items: center;
              gap: 14px;
            }
            .meta-label {
              font-size: 11px;
              color: var(--text-muted);
              text-transform: uppercase;
              letter-spacing: 0.08em;
              display: block;
            }
            .meta-value {
              font-size: 13px;
              letter-spacing: 0.04em;
            }
            .meta-buttons {
              display: flex;
              gap: 10px;
            }
            button {
              font-size: 12px;
              text-transform: uppercase;
              letter-spacing: 0.08em;
              border-radius: 999px;
              padding: 10px 18px;
              border: none;
              cursor: pointer;
              transition: background 0.2s ease, color 0.2s ease;
            }
            .ghost {
              background: transparent;
              color: var(--text-muted);
              border: 1px solid var(--panel-border);
            }
            .ghost:hover { color: #fff; }
            .primary {
              background: linear-gradient(135deg, #ff7a18, #af002d);
              color: #fff;
              box-shadow: 0 10px 20px rgba(175, 0, 45, 0.5);
            }
            .primary:hover { filter: brightness(1.1); }
            .quote-card.move-up .price-glow,
            .quote-card.move-up .last-value { color: #4ade80; }
            .quote-card.move-down .price-glow,
            .quote-card.move-down .last-value { color: #f87171; }
          </style>
        </head>
        <body>
          <div class="terminal-chrome">
            <div class="chrome-bar">
              <div>Fabric UI Pricer</div>
              <div class="chrome-env">Standalone Demo</div>
            </div>
            <div class="chrome-sub">
              <div>WebSocket feed</div>
              <div>Interval <strong style="color:#fff;">%d ms</strong></div>
              <div>Port <strong style="color:#fff;">%d</strong></div>
            </div>
          </div>
          <main>
            <section class="grid" id="price-grid"></section>
          </main>

          <script>
            const cards = new Map();
            const grid = document.getElementById('price-grid');

            function formatNumber(value) {
              return Number.parseFloat(value).toFixed(2);
            }

            function ensureCard(symbol) {
              let card = cards.get(symbol);
              if (card) {
                return card;
              }
              card = document.createElement('article');
              card.className = 'quote-card';
              card.dataset.symbol = symbol;
              card.dataset.last = '0';
              card.innerHTML = `
                <header class="quote-header">
                  <div>
                    <div class="ticker">${symbol}</div>
                    <div class="label">Streaming quote</div>
                  </div>
                  <div class="last-block">
                    <div class="last-label">Last</div>
                    <div class="last-value" data-field="last">0.00</div>
                  </div>
                </header>
                <section class="price-stage">
                  <div class="price-glow" data-field="last-big">0.00</div>
                  <div class="side side-bid">
                    <span>Bid</span>
                    <strong data-field="bid">0.00</strong>
                  </div>
                  <div class="side side-ask">
                    <span>Ask</span>
                    <strong data-field="ask">0.00</strong>
                  </div>
                </section>
                <section class="meta">
                  <div>
                    <span class="meta-label">Last update</span>
                    <span class="meta-value" data-field="time">--</span>
                  </div>
                  <div class="meta-buttons">
                    <button class="ghost">Missed</button>
                    <button class="primary">Accept</button>
                  </div>
                </section>`;
              grid.appendChild(card);
              cards.set(symbol, card);
              return card;
            }

            function updateCard(quote) {
              const card = ensureCard(quote.symbol);
              const previous = Number.parseFloat(card.dataset.last);
              card.dataset.last = quote.lastPrice;
              const direction = quote.lastPrice > previous ? 'move-up' : quote.lastPrice < previous ? 'move-down' : '';

              card.querySelector('[data-field="last"]').textContent = formatNumber(quote.lastPrice);
              card.querySelector('[data-field="last-big"]').textContent = formatNumber(quote.lastPrice);
              card.querySelector('[data-field="bid"]').textContent = formatNumber(quote.bidPrice);
              card.querySelector('[data-field="ask"]').textContent = formatNumber(quote.askPrice);
              card.querySelector('[data-field="time"]').textContent = quote.lastUpdateTime;

              card.classList.remove('move-up', 'move-down');
              if (direction) {
                card.classList.add(direction);
                setTimeout(() => card.classList.remove(direction), 600);
              }
            }

            function connect() {
              const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
              const socket = new WebSocket(`${wsProtocol}://${window.location.host}/ws/prices`);
              socket.onmessage = (event) => {
                const quotes = JSON.parse(event.data);
                quotes.forEach(updateCard);
              };

              socket.onclose = () => {
                setTimeout(connect, 1000);
              };

              socket.onerror = () => socket.close();
            }

            connect();
          </script>
        </body>
        </html>
        """
        .formatted(TICK_MILLIS, PORT);
  }

  private static final class Price {
    private final String symbol;
    private double lastPrice;
    private double bidPrice;
    private double askPrice;
    private String lastUpdateTime;

    private Price(String symbol, double basePrice) {
      this.symbol = symbol;
      this.lastPrice = basePrice;
      this.bidPrice = basePrice - 0.05;
      this.askPrice = basePrice + 0.05;
      this.lastUpdateTime = TIMESTAMP.format(Instant.now());
    }

    synchronized void update(double delta) {
      lastPrice = Math.max(0.01, lastPrice + delta);
      bidPrice = lastPrice - 0.05;
      askPrice = lastPrice + 0.05;
      lastUpdateTime = TIMESTAMP.format(Instant.now());
    }

    synchronized PriceView view() {
      return new PriceView(symbol, lastPrice, bidPrice, askPrice, lastUpdateTime);
    }
  }

  private record PriceView(String symbol, double lastPrice, double bidPrice, double askPrice, String lastUpdateTime) {}
}
