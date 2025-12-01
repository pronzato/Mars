package org.pronzato.fabric.test.splunk;

import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.Service;
import com.splunk.ServiceArgs;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Minimal Splunk Java SDK smoke test: connect, run a search, echo a few results.
 */
public final class TestSplunk {

    private static final String SPLUNK_HOST = "splunk.example.com";
    private static final int SPLUNK_PORT = 8089;                    // mgmt port
    private static final String SPLUNK_SCHEME = "https";
    private static final String SPLUNK_USERNAME = "your_user";
    private static final String SPLUNK_PASSWORD = "your_password";
    private static final String SPLUNK_SEARCH = "search index=your_index_name earliest=-15m@m latest=now | head 10";
    private static final int TIMEOUT_SECONDS = 60;
    private static final boolean TRUST_ALL_CERTS = false;           // if true, configure SDK to trust-all (LAB ONLY)

    private static final int SAMPLE_EVENT_LIMIT = 10;

    private TestSplunk() {
        // utility
    }

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("Splunk Java SDK Smoke Test");
        System.out.println("============================================================");

        Instant start = Instant.now();
        System.out.println("Search starting at " + start);
        try {
            configureTrustAllIfNeeded();

            ServiceArgs serviceArgs = new ServiceArgs();
            serviceArgs.setHost(SPLUNK_HOST);
            serviceArgs.setPort(SPLUNK_PORT);
            serviceArgs.setScheme(SPLUNK_SCHEME);
            serviceArgs.setUsername(SPLUNK_USERNAME);
            serviceArgs.setPassword(SPLUNK_PASSWORD);

            System.out.printf(Locale.US, "Connecting to %s://%s:%d ...%n", SPLUNK_SCHEME, SPLUNK_HOST, SPLUNK_PORT);
            Service service = Service.connect(serviceArgs);
            service.login();
            System.out.println("Authenticated. Session token length=" + (service.getToken() == null ? 0 : service.getToken().length()));

            Job job = submitSearch(service);
            waitForCompletion(job, Duration.ofSeconds(TIMEOUT_SECONDS));
            SearchResultSummary summary = collectResults(job);

            Instant end = Instant.now();
            long elapsedMs = Duration.between(start, end).toMillis();
            System.out.println("------------------------------------------------------------");
            System.out.println("Search completed at " + end + " (elapsed " + elapsedMs + " ms)");
            System.out.println("Total events: " + summary.totalEvents);
            if (summary.totalEvents == 0) {
                System.out.println("No events returned. Verify the SPL and index filters.");
            } else {
                System.out.println("Sample events (up to " + SAMPLE_EVENT_LIMIT + "):");
                for (int i = 0; i < summary.samples.size(); i++) {
                    System.out.printf(Locale.US, "[%02d] %s%n", i + 1, summary.samples.get(i));
                }
            }
            System.exit(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Splunk smoke test interrupted: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Splunk smoke test failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Job submitSearch(Service service) {
        System.out.println("Submitting search: " + SPLUNK_SEARCH);
        JobArgs jobArgs = new JobArgs();
        jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
        Job job = service.getJobs().create(SPLUNK_SEARCH, jobArgs);
        System.out.println("Search SID: " + job.getSid());
        return job;
    }

    private static void waitForCompletion(Job job, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (!job.isDone()) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Search did not finish within " + timeout.toSeconds() + " seconds");
            }
            Thread.sleep(1000);
            job.refresh();
            System.out.printf(Locale.US, "Job %s progress: %.2f%%%n", job.getSid(), job.getDoneProgress() * 100.0);
        }
    }

    private static SearchResultSummary collectResults(Job job) throws IOException {
        JobResultsArgs resultsArgs = new JobResultsArgs();
        resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);
        resultsArgs.setCount(0); // pull everything

        int totalEvents = 0;
        List<String> samples = new ArrayList<>();
        try (InputStream stream = job.getResults(resultsArgs)) {
            ResultsReaderJson reader = new ResultsReaderJson(stream);
            try {
                Map<String, String> event;
                while ((event = reader.getNextEvent()) != null) {
                    totalEvents++;
                    if (samples.size() < SAMPLE_EVENT_LIMIT) {
                        samples.add(abbreviateEvent(event));
                    }
                }
            } finally {
                reader.close();
            }
        }
        return new SearchResultSummary(totalEvents, samples);
    }

    private static String abbreviateEvent(Map<String, String> event) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : event.entrySet()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(entry.getKey()).append('=');
            String value = entry.getValue();
            if (value == null) {
                builder.append("<null>");
            } else if (value.length() > 120) {
                builder.append(value, 0, 117).append("...");
            } else {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private static void configureTrustAllIfNeeded() throws NoSuchAlgorithmException, KeyManagementException {
        if (!TRUST_ALL_CERTS) {
            return;
        }
        System.out.println("WARNING: TRUST_ALL_CERTS=true disables TLS validation. Use only in a lab environment.");
        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // insecure on purpose
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // insecure on purpose
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        HttpService.setSSLSocketFactory(sslContext.getSocketFactory());
        HttpService.setValidateCertificates(false);
    }

    private static final class SearchResultSummary {
        private final int totalEvents;
        private final List<String> samples;

        private SearchResultSummary(int totalEvents, List<String> samples) {
            this.totalEvents = totalEvents;
            this.samples = samples;
        }
    }
}

