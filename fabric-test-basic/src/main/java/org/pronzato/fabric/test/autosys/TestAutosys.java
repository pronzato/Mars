package org.pronzato.fabric.test.autosys;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Minimal AutoSys iXp CLI smoke test: login, list jobs, logout.
 */
public final class TestAutosys {

    private static final String IXP_EXE_PATH = "C:\\Program Files\\Broadcom\\iXp\\bin\\ixp.exe";
    private static final String IXP_SERVER_URL = "https://ixp-server.example.com:8443";
    private static final String IXP_USERNAME = "your_user";
    private static final String IXP_PASSWORD = "your_password";
    private static final String JOB_NAME_FILTER = "MYAPP_*";    // optional; pass if supported by ixp list
    private static final String OUTPUT_FORMAT = "json";         // prefer json; fallback to table if needed

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(60);
    private static final int JOB_LIST_PREVIEW_CHARS = 2000;
    private static final int STDERR_TAIL_CHARS = 500;

    private TestAutosys() {
        // utility
    }

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("AutoSys iXp CLI Smoke Test");
        System.out.println("============================================================");

        if (!isWindows()) {
            System.err.println("This smoke test only runs on Windows hosts. Detected os.name=" + System.getProperty("os.name"));
            System.exit(1);
        }

        Instant suiteStart = Instant.now();
        try {
            CommandResult login = runCommand("Login", buildLoginCommand(), true);
            CommandResult list = runCommand("Job list", buildJobListCommand(), false);
            handleJobListOutput(list);
            CommandResult logout = runCommand("Logout", buildLogoutCommand(), false);

            long totalMillis = Duration.between(suiteStart, Instant.now()).toMillis();
            System.out.printf("Completed AutoSys smoke test in %d ms (login=%d ms, list=%d ms, logout=%d ms)%n",
                    totalMillis,
                    login.duration.toMillis(),
                    list.duration.toMillis(),
                    logout.duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Smoke test interrupted: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Smoke test failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private static void handleJobListOutput(CommandResult jobList) {
        String stdout = jobList.stdout == null ? "" : jobList.stdout;
        System.out.println("-- Job list preview --");
        if (stdout.isEmpty()) {
            System.out.println("(no stdout returned)");
        } else {
            String preview = truncate(stdout, JOB_LIST_PREVIEW_CHARS);
            System.out.println(preview);
            if (stdout.length() > JOB_LIST_PREVIEW_CHARS) {
                System.out.println("... [truncated]");
            }
        }

        try {
            Path tempFile = Files.createTempFile("ixp-job-list-", OUTPUT_FORMAT.equalsIgnoreCase("json") ? ".json" : ".txt");
            Files.writeString(tempFile, stdout, StandardCharsets.UTF_8);
            System.out.println("Full job list output saved to: " + tempFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not persist job list output: " + e.getMessage());
        }

        if ("json".equalsIgnoreCase(OUTPUT_FORMAT)) {
            summarizeJson(stdout);
        }
    }

    private static void summarizeJson(String stdout) {
        int running = countOccurrences(stdout, "\"RUNNING\"");
        int failed = countOccurrences(stdout, "\"FAILED\"");
        System.out.printf("JSON state summary (string search): RUNNING=%d, FAILED=%d%n", running, failed);
    }

    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null || haystack.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static CommandResult runCommand(String label, List<String> command, boolean maskPassword) throws IOException, InterruptedException {
        System.out.println("-- " + label + " --");
        System.out.println("Command: " + renderCommand(command, maskPassword));
        CommandResult result = execute(command, COMMAND_TIMEOUT);
        if (result.exitCode != 0) {
            System.err.printf("%s failed with exit code %d%n", label, result.exitCode);
            if (!result.stderr.isBlank()) {
                System.err.println("stderr tail (last " + STDERR_TAIL_CHARS + " chars):");
                System.err.println(tail(result.stderr, STDERR_TAIL_CHARS));
            }
            System.exit(result.exitCode);
        }
        System.out.printf("%s succeeded in %d ms%n", label, result.duration.toMillis());
        return result;
    }

    private static String renderCommand(List<String> command, boolean maskPassword) {
        List<String> printable = new ArrayList<>(command.size());
        boolean hideNext = false;
        for (String token : command) {
            String value = token;
            if (maskPassword) {
                if (hideNext) {
                    value = "****";
                    hideNext = false;
                } else if ("--password".equals(token)) {
                    hideNext = true;
                }
            }
            printable.add(formatArg(value));
        }
        return String.join(" ", printable);
    }

    private static String formatArg(String value) {
        if (value.contains(" ") || value.contains("\"")) {
            return '"' + value.replace("\"", "\\\"") + '"';
        }
        return value;
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }

    private static String tail(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(text.length() - maxChars);
    }

    private static List<String> buildLoginCommand() {
        return Arrays.asList(
                IXP_EXE_PATH,
                "login",
                "--server", IXP_SERVER_URL,
                "--user", IXP_USERNAME,
                "--password", IXP_PASSWORD
        );
    }

    private static List<String> buildJobListCommand() {
        List<String> command = new ArrayList<>();
        command.add(IXP_EXE_PATH);
        command.add("job");
        command.add("list");
        command.add("--output");
        command.add(OUTPUT_FORMAT);
        if (JOB_NAME_FILTER != null && !JOB_NAME_FILTER.isBlank()) {
            command.add("--name");
            command.add(JOB_NAME_FILTER);
        }
        return command;
    }

    private static List<String> buildLogoutCommand() {
        return Arrays.asList(IXP_EXE_PATH, "logout");
    }

    private static CommandResult execute(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        long start = System.nanoTime();
        Process process = builder.start();

        StreamCollector stdoutCollector = new StreamCollector(process.getInputStream());
        StreamCollector stderrCollector = new StreamCollector(process.getErrorStream());
        Thread stdoutThread = new Thread(stdoutCollector, "ixp-stdout");
        Thread stderrThread = new Thread(stderrCollector, "ixp-stderr");
        stdoutThread.start();
        stderrThread.start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            stdoutThread.join();
            stderrThread.join();
            throw new IOException("Command timed out after " + timeout.toSeconds() + " seconds");
        }

        stdoutThread.join();
        stderrThread.join();
        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        return new CommandResult(process.exitValue(), stdoutCollector.content(), stderrCollector.content(), duration);
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final Duration duration;

        private CommandResult(int exitCode, String stdout, String stderr, Duration duration) {
            this.exitCode = exitCode;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
            this.duration = duration;
        }
    }

    private static final class StreamCollector implements Runnable {
        private final InputStream source;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private StreamCollector(InputStream source) {
            this.source = source;
        }

        @Override
        public void run() {
            try (InputStream in = source; ByteArrayOutputStream out = buffer) {
                in.transferTo(out);
            } catch (IOException e) {
                try {
                    buffer.write(("[stream error] " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }

        private String content() {
            synchronized (buffer) {
                return buffer.toString(StandardCharsets.UTF_8);
            }
        }
    }
}
