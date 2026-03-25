package org.pronzato.fabric.lite.patch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PatchGenerator {
  private static final String DEFAULT_PATCH_RELATIVE = "docs/patches/patch.txt";
  private static final String PATCH_FORMAT = "FABRIC_PATCH_V1";
  private static final String CONTENT_ENCODING_BASE64 = "BASE64";
  private static final String WIKI_DOCS_PREFIX = "docs/wiki/";
  private static final String FEATHER_ICONS_PREFIX =
      "fabric-lite-stitch/src/main/resources/META-INF/resources/images/icons/feather/";
  private static final String STUDIO_MODULE_PREFIX = "fabric-lite-studio/";
  private static final String PATCH_GENERATOR_SOURCE =
      "fabric-lite-config/src/main/java/org/pronzato/fabric/lite/patch/PatchGenerator.java";
  private static final String PATCH_INSTALLER_SOURCE =
      "fabric-lite-config/src/main/java/org/pronzato/fabric/lite/patch/PatchInstaller.java";

  private PatchGenerator() {}

  public static void main(String[] args) {
    if (args.length > 2) {
      System.err.println(
          "Usage: java org.pronzato.fabric.lite.patch.PatchGenerator [patch.txt] [repoRoot]");
      System.exit(2);
    }

    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path repoRoot;
    Path patchPath;
    try {
      if (args.length == 0) {
        repoRoot = findRepoRoot(cwd);
        patchPath = repoRoot.resolve(DEFAULT_PATCH_RELATIVE).normalize();
      } else if (args.length == 1) {
        repoRoot = findRepoRoot(cwd);
        patchPath = resolveFrom(cwd, args[0]);
      } else {
        patchPath = resolveFrom(cwd, args[0]);
        repoRoot = resolveFrom(cwd, args[1]);
      }

      generatePatch(repoRoot, patchPath);
    } catch (Exception e) {
      System.err.println("Patch generation failed: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void generatePatch(Path repoRoot, Path patchPath) throws IOException {
    if (!Files.isDirectory(repoRoot)) {
      throw new IllegalArgumentException("Repo root is not a directory: " + repoRoot);
    }

    System.out.println("Collecting changed files from git...");
    String baselineSha = runGit(repoRoot, "--no-pager", "rev-parse", "HEAD").trim();
    List<String> diffLines =
        runGitLines(
            repoRoot,
            "--no-pager",
            "-c",
            "core.safecrlf=false",
            "diff",
            "--name-status",
            "--no-ext-diff",
            "HEAD");
    List<String> untrackedLines =
        runGitLines(repoRoot, "--no-pager", "ls-files", "--others", "--exclude-standard");

    Set<String> writeSet = new HashSet<>();
    Set<String> deleteSet = new HashSet<>();

    for (String line : diffLines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      String[] parts = line.split("\t");
      if (parts.length < 2) {
        continue;
      }
      String status = parts[0].trim();
      if (status.startsWith("D")) {
        String deletedPath = normalizePath(parts[parts.length - 1]);
        if (isPatchablePath(deletedPath)) {
          deleteSet.add(deletedPath);
        }
      } else if (status.startsWith("R") && parts.length >= 3) {
        // Rename: delete old path, write new path.
        String oldPath = normalizePath(parts[1]);
        String newPath = normalizePath(parts[2]);
        if (isPatchablePath(oldPath)) {
          deleteSet.add(oldPath);
        }
        if (isPatchablePath(newPath)) {
          writeSet.add(newPath);
        }
      } else {
        String changedPath = normalizePath(parts[parts.length - 1]);
        if (isPatchablePath(changedPath)) {
          writeSet.add(changedPath);
        }
      }
    }

    for (String line : untrackedLines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      String path = normalizePath(line);
      if (isPatchablePath(path)) {
        writeSet.add(path);
      }
    }

    List<PatchEntry> entries = new ArrayList<>();
    for (String path : writeSet) {
      entries.add(PatchEntry.write(path));
    }
    for (String path : deleteSet) {
      if (!writeSet.contains(path)) {
        entries.add(PatchEntry.delete(path));
      }
    }

    entries.sort((a, b) -> {
      int c1 = a.module.compareTo(b.module);
      if (c1 != 0) {
        return c1;
      }
      int c2 = a.packageName.compareTo(b.packageName);
      if (c2 != 0) {
        return c2;
      }
      int c3 = a.fileName.compareTo(b.fileName);
      if (c3 != 0) {
        return c3;
      }
      return a.action.compareTo(b.action);
    });

    int writeCount = (int) entries.stream().filter(e -> e.action == Action.WRITE).count();
    int deleteCount = (int) entries.stream().filter(e -> e.action == Action.DELETE).count();

    Path parent = patchPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    try (BufferedWriter writer = Files.newBufferedWriter(patchPath, StandardCharsets.UTF_8)) {
      writer.write("PATCH_FORMAT: " + PATCH_FORMAT);
      writer.newLine();
      writer.write("BASELINE_SHA: " + baselineSha);
      writer.newLine();
      writer.write("GENERATED_AT_UTC: " + Instant.now().toString());
      writer.newLine();
      writer.write("TOTAL_FILES: " + entries.size());
      writer.newLine();
      writer.write("WRITE_COUNT: " + writeCount);
      writer.newLine();
      writer.write("DELETE_COUNT: " + deleteCount);
      writer.newLine();
      writer.newLine();

      if (entries.isEmpty()) {
        writer.write("NO_CHANGES");
        writer.newLine();
      } else {
        for (PatchEntry entry : entries) {
          writer.write("BEGIN_FILE");
          writer.newLine();
          writer.write("ACTION: " + entry.action.name());
          writer.newLine();
          writer.write("PATH: " + entry.path);
          writer.newLine();
          writer.write("MODULE: " + entry.module);
          writer.newLine();
          writer.write("PACKAGE: " + entry.packageName);
          writer.newLine();

          if (entry.action == Action.WRITE) {
            Path sourceFile = repoRoot.resolve(entry.path).normalize();
            if (!Files.isRegularFile(sourceFile)) {
              throw new IllegalArgumentException("WRITE entry source file missing: " + entry.path);
            }
            byte[] contentBytes = Files.readAllBytes(sourceFile);
            String content = Base64.getEncoder().encodeToString(contentBytes);
            writer.write("CONTENT_ENCODING: " + CONTENT_ENCODING_BASE64);
            writer.newLine();
            writer.write("CONTENT_START");
            writer.newLine();
            writer.write(content);
            if (!content.endsWith("\n") && !content.endsWith("\r")) {
              writer.newLine();
            }
            writer.write("CONTENT_END");
            writer.newLine();
          }

          writer.write("END_FILE");
          writer.newLine();
          writer.newLine();
        }
      }
    }

    System.out.println("Generated patch: " + patchPath);
    System.out.println("Baseline SHA    : " + baselineSha);
    System.out.println("WRITE entries   : " + writeCount);
    System.out.println("DELETE entries  : " + deleteCount);
    System.out.println("TOTAL entries   : " + entries.size());
    System.out.println();
    System.out.println("Generated entries");
    if (entries.isEmpty()) {
      System.out.println("  (none)");
    } else {
      String currentModule = null;
      for (PatchEntry entry : entries) {
        if (!entry.module.equals(currentModule)) {
          currentModule = entry.module;
          String label = currentModule.isBlank() ? "(root)" : currentModule;
          System.out.println("[" + label + "]");
        }
        System.out.println("  " + entry.action.name() + " " + entry.path);
      }
    }
  }

  private static String normalizePath(String pathText) {
    return pathText.trim().replace('\\', '/');
  }

  private static boolean isPatchablePath(String path) {
    String normalized = normalizePath(path);
    if (PATCH_GENERATOR_SOURCE.equals(normalized) || PATCH_INSTALLER_SOURCE.equals(normalized)) {
      return false;
    }
    if (normalized.startsWith(WIKI_DOCS_PREFIX)) {
      return true;
    }
    if (normalized.startsWith(FEATHER_ICONS_PREFIX)) {
      return true;
    }
    if (normalized.endsWith(".java")) {
      return !normalized.startsWith(STUDIO_MODULE_PREFIX);
    }
    return normalized.contains("/src/main/resources/");
  }

  private static String runGit(Path repoRoot, String... args) throws IOException {
    Process process = startGit(repoRoot, args);
    try {
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exit = process.waitFor();
      if (exit != 0) {
        throw new IOException("git command failed (" + String.join(" ", args) + "): " + output.trim());
      }
      return output;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("git command interrupted", e);
    }
  }

  private static List<String> runGitLines(Path repoRoot, String... args) throws IOException {
    String output = runGit(repoRoot, args);
    String[] lines = output.split("\\R");
    List<String> result = new ArrayList<>();
    for (String line : lines) {
      if (!line.isBlank()) {
        result.add(line);
      }
    }
    return result;
  }

  private static Process startGit(Path repoRoot, String... args) throws IOException {
    List<String> command = new ArrayList<>();
    command.add("git");
    for (String arg : args) {
      command.add(arg);
    }
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(repoRoot.toFile());
    pb.redirectErrorStream(true);
    return pb.start();
  }

  private static Path findRepoRoot(Path start) {
    Path current = start;
    while (current != null) {
      Path gitDir = current.resolve(".git");
      Path patchFile = current.resolve(DEFAULT_PATCH_RELATIVE);
      if (Files.exists(gitDir) || Files.exists(patchFile)) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalArgumentException(
        "Could not find repo root (missing .git and " + DEFAULT_PATCH_RELATIVE + ") from " + start);
  }

  private static Path resolveFrom(Path base, String pathText) {
    Path path = Paths.get(pathText);
    if (!path.isAbsolute()) {
      path = base.resolve(path);
    }
    return path.toAbsolutePath().normalize();
  }

  private enum Action {
    WRITE,
    DELETE
  }

  private static final class PatchEntry {
    private final Action action;
    private final String path;
    private final String module;
    private final String packageName;
    private final String fileName;

    private PatchEntry(Action action, String path) {
      this.action = action;
      this.path = path;
      this.module = module(path);
      this.packageName = packageName(path);
      this.fileName = fileName(path);
    }

    private static PatchEntry write(String path) {
      return new PatchEntry(Action.WRITE, path);
    }

    private static PatchEntry delete(String path) {
      return new PatchEntry(Action.DELETE, path);
    }

    private static String module(String path) {
      int slash = path.indexOf('/');
      return slash > 0 ? path.substring(0, slash) : "";
    }

    private static String packageName(String path) {
      String norm = path.replace('\\', '/');
      String marker = "/java/";
      int javaIdx = norm.indexOf(marker);
      if (javaIdx >= 0) {
        int start = javaIdx + marker.length();
        int lastSlash = norm.lastIndexOf('/');
        if (lastSlash > start) {
          return norm.substring(start, lastSlash).replace('/', '.');
        }
      }
      int slash = norm.lastIndexOf('/');
      return slash > 0 ? norm.substring(0, slash) : "";
    }

    private static String fileName(String path) {
      int slash = path.lastIndexOf('/');
      return slash >= 0 ? path.substring(slash + 1) : path;
    }
  }
}



package org.pronzato.fabric.lite.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

public final class PatchInstaller {
  private static final String DEFAULT_PATCH_RELATIVE = "docs/patches/patch.txt";
  private static final String CONTENT_ENCODING_BASE64 = "BASE64";
  private static final String CONTENT_ENCODING_UTF8 = "UTF-8";
  private static final String WIKI_DOCS_PREFIX = "docs/wiki/";
  private static final String FEATHER_ICONS_PREFIX =
      "fabric-lite-stitch/src/main/resources/META-INF/resources/images/icons/feather/";
  private static final String STUDIO_MODULE_PREFIX = "fabric-lite-studio/";

  private PatchInstaller() {}

  public static void main(String[] args) {
    if (true) {
      System.out.println("running installer not allowed in this environment");
      return;
    }
    if (args.length > 2) {
      System.err.println(
          "Usage: java org.pronzato.fabric.lite.patch.PatchInstaller [patch.txt] [repoRoot]");
      System.exit(2);
    }

    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path repoRoot;
    Path patchPath;

    try {
      if (args.length == 0) {
        repoRoot = findRepoRoot(cwd);
        patchPath = repoRoot.resolve(DEFAULT_PATCH_RELATIVE).normalize();
      } else if (args.length == 1) {
        repoRoot = findRepoRoot(cwd);
        patchPath = resolveFrom(cwd, args[0]);
      } else {
        patchPath = resolveFrom(cwd, args[0]);
        repoRoot = resolveFrom(cwd, args[1]);
      }
    } catch (Exception e) {
      System.err.println("Patch initialization failed: " + e.getMessage());
      System.exit(2);
      return;
    }

    try {
      runInstaller(patchPath, repoRoot);
    } catch (Exception e) {
      System.err.println("Patch installation failed: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void runInstaller(Path patchPath, Path repoRoot) throws IOException {
    if (!Files.exists(patchPath)) {
      throw new IllegalArgumentException("Patch file does not exist: " + patchPath);
    }
    if (!Files.isDirectory(repoRoot)) {
      throw new IllegalArgumentException("Repo root is not a directory: " + repoRoot);
    }

    int writes = 0;
    int deletes = 0;
    int deleteSkipped = 0;
    int unsupportedSkipped = 0;
    int errors = 0;
    boolean noChanges = false;

    try (BufferedReader reader = Files.newBufferedReader(patchPath, StandardCharsets.UTF_8)) {
      String line;
      int lineNo = 0;
      boolean started = false;

      while ((line = reader.readLine()) != null) {
        lineNo++;
        if (line.isBlank()) {
          continue;
        }
        if ("NO_CHANGES".equals(line)) {
          noChanges = true;
          break;
        }
        if ("BEGIN_FILE".equals(line)) {
          started = true;
          PatchEntry entry = readEntry(reader, lineNo);
          lineNo = entry.lastLineNumber;
          if (!isPatchablePath(entry.path)) {
            unsupportedSkipped++;
            System.out.println("SKIP_UNSUPPORTED " + entry.path);
            continue;
          }
          Path target = resolveTarget(repoRoot, entry.path);

          try {
            if (entry.action == Action.WRITE) {
              Path parent = target.getParent();
              if (parent != null) {
                Files.createDirectories(parent);
              }
              Files.write(
                  target,
                  entry.contentBytes,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.TRUNCATE_EXISTING);
              writes++;
              System.out.println("WRITE  " + entry.path);
            } else {
              if (Files.exists(target)) {
                Files.delete(target);
                deletes++;
                System.out.println("DELETE " + entry.path);
              } else {
                deleteSkipped++;
                System.out.println("SKIP_DELETE_MISSING " + entry.path);
              }
            }
          } catch (Exception e) {
            errors++;
            System.err.println("ERROR applying " + entry.action + " for " + entry.path + ": " + e.getMessage());
          }
          continue;
        }

        if (!started) {
          continue;
        }
        throw new IllegalArgumentException("Malformed patch at line " + lineNo + ": expected BEGIN_FILE or EOF");
      }
    }

    if (noChanges) {
      System.out.println("Patch contains NO_CHANGES. Nothing to apply.");
    }

    System.out.println();
    System.out.println("Patch apply summary");
    System.out.println("  repoRoot            : " + repoRoot);
    System.out.println("  patchFile           : " + patchPath);
    System.out.println("  files written       : " + writes);
    System.out.println("  files deleted       : " + deletes);
    System.out.println("  delete skipped      : " + deleteSkipped);
    System.out.println("  unsupported skipped : " + unsupportedSkipped);
    System.out.println("  errors              : " + errors);

    if (errors > 0) {
      throw new IllegalStateException("Patch completed with " + errors + " error(s).");
    }
  }

  private static Path findRepoRoot(Path start) {
    Path current = start;
    while (current != null) {
      Path candidatePatch = current.resolve(DEFAULT_PATCH_RELATIVE).normalize();
      if (Files.exists(candidatePatch)) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalArgumentException(
        "Could not find repo root containing " + DEFAULT_PATCH_RELATIVE + " starting from " + start);
  }

  private static Path resolveFrom(Path base, String pathText) {
    Path path = Paths.get(pathText);
    if (!path.isAbsolute()) {
      path = base.resolve(path);
    }
    return path.toAbsolutePath().normalize();
  }

  private static PatchEntry readEntry(BufferedReader reader, int beginLineNo) throws IOException {
    int lineNo = beginLineNo;

    String actionLine = reader.readLine();
    lineNo++;
    String pathLine = reader.readLine();
    lineNo++;
    String moduleLine = reader.readLine();
    lineNo++;
    String packageLine = reader.readLine();
    lineNo++;

    String actionValue = parseField(actionLine, "ACTION", lineNo - 3);
    String pathValue = parseField(pathLine, "PATH", lineNo - 2);
    parseField(moduleLine, "MODULE", lineNo - 1); // parsed for validation; not used by installer
    parseField(packageLine, "PACKAGE", lineNo); // parsed for validation; not used by installer

    Action action;
    if ("WRITE".equals(actionValue)) {
      action = Action.WRITE;
    } else if ("DELETE".equals(actionValue)) {
      action = Action.DELETE;
    } else {
      throw new IllegalArgumentException("Invalid ACTION at line " + (lineNo - 3) + ": " + actionValue);
    }

    byte[] contentBytes = new byte[0];
    if (action == Action.WRITE) {
      String contentEncoding = CONTENT_ENCODING_UTF8;
      String contentStart = reader.readLine();
      lineNo++;
      if (contentStart != null && contentStart.startsWith("CONTENT_ENCODING:")) {
        contentEncoding = parseField(contentStart, "CONTENT_ENCODING", lineNo);
        contentStart = reader.readLine();
        lineNo++;
      }
      if (!"CONTENT_START".equals(contentStart)) {
        throw new IllegalArgumentException("Malformed patch at line " + lineNo + ": expected CONTENT_START");
      }

      StringBuilder content = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        lineNo++;
        if ("CONTENT_END".equals(line)) {
          break;
        }
        content.append(line).append('\n');
      }
      if (content.length() > 0) {
        content.setLength(content.length() - 1);
      }
      if (line == null) {
        throw new IllegalArgumentException("Malformed patch: missing CONTENT_END for " + pathValue);
      }

      contentBytes = decodeContent(content.toString(), contentEncoding, pathValue);
    }

    String endFile = reader.readLine();
    lineNo++;
    if (!"END_FILE".equals(endFile)) {
      throw new IllegalArgumentException("Malformed patch at line " + lineNo + ": expected END_FILE");
    }

    return new PatchEntry(action, pathValue, contentBytes, lineNo);
  }

  private static String parseField(String line, String key, int lineNo) {
    String prefix = key + ":";
    if (line == null || !line.startsWith(prefix)) {
      throw new IllegalArgumentException("Malformed patch at line " + lineNo + ": expected " + key + ":");
    }
    return line.substring(prefix.length()).trim();
  }

  private static boolean isPatchablePath(String path) {
    String normalized = path.trim().replace('\\', '/');
    if (normalized.startsWith(WIKI_DOCS_PREFIX)) {
      return true;
    }
    if (normalized.startsWith(FEATHER_ICONS_PREFIX)) {
      return true;
    }
    if (normalized.endsWith(".java")) {
      return !normalized.startsWith(STUDIO_MODULE_PREFIX);
    }
    return normalized.contains("/src/main/resources/");
  }

  private static Path resolveTarget(Path repoRoot, String relativePath) {
    String normalizedRelative = relativePath.replace('\\', '/');
    Path target = repoRoot.resolve(normalizedRelative).normalize();
    if (!target.startsWith(repoRoot)) {
      throw new IllegalArgumentException("Unsafe path outside repo root: " + relativePath);
    }
    return target;
  }

  private static byte[] decodeContent(String content, String encoding, String pathValue) {
    if (CONTENT_ENCODING_BASE64.equalsIgnoreCase(encoding)) {
      try {
        return Base64.getDecoder().decode(content.replaceAll("\\s+", ""));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid BASE64 content for " + pathValue + ": " + e.getMessage(), e);
      }
    }
    if (CONTENT_ENCODING_UTF8.equalsIgnoreCase(encoding)) {
      return content.getBytes(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException("Unsupported CONTENT_ENCODING for " + pathValue + ": " + encoding);
  }

  private enum Action {
    WRITE,
    DELETE
  }

  private static final class PatchEntry {
    private final Action action;
    private final String path;
    private final byte[] contentBytes;
    private final int lastLineNumber;

    private PatchEntry(Action action, String path, byte[] contentBytes, int lastLineNumber) {
      this.action = action;
      this.path = path;
      this.contentBytes = contentBytes;
      this.lastLineNumber = lastLineNumber;
    }
  }
}







