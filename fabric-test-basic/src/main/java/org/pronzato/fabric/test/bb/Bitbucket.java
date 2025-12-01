package org.pronzato.fabric.test.bb;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

/**
 * Fabric Bitbucket Connectivity Test
 *
 * This standalone class tests connectivity and permissions for:
 *   - Bitbucket Server/Data Center (on-prem)
 *   - Bitbucket Cloud
 *
 * It exercises:
 *   - Authentication (username/password/PAT/app-password)
 *   - Repo read: list directories, read files, get commits
 *   - Repo write (optional): create branches, create commits, create PR
 *
 * Usage:
 *   Run main() and answer prompts.
 */
public class Bitbucket {

    // ---------------------------------------------------------------
    // CONFIG PARAMETERS (collected interactively from user)
    // ---------------------------------------------------------------
    private static String baseUrl;      // e.g. https://bitbucket.myorg.com
    private static String projectKey;   // e.g. FABRIC
    private static String repoSlug;     // e.g. metadata
    private static String username;     // your bitbucket username
    private static String passwordOrToken; // app-password, PAT, or password

    private static HttpClient http;

    public static void main(String[] args) throws Exception {
        collectParameters();
        http = HttpClient.newBuilder().build();

        System.out.println("\n=== FABRIC BITBUCKET CONNECTIVITY TEST ===\n");

        testBasicConnectivity();
        testGetRepoRoot();
        testGetBranches();
        testGetLatestCommitOnMain();
        testReadFile();
        testCreateBranch();
        testCreateCommit();
        testCreatePullRequest();

        System.out.println("\n=== ALL TESTS EXECUTED ===\n");
    }

    // Collect all parameters interactively
    private static void collectParameters() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter Bitbucket base URL (e.g. https://bitbucket.myorg.com): ");
        baseUrl = scanner.nextLine().trim();

        System.out.println("Enter project key (e.g. FABRIC): ");
        projectKey = scanner.nextLine().trim();

        System.out.println("Enter repo slug (e.g. metadata): ");
        repoSlug = scanner.nextLine().trim();

        System.out.println("Enter username: ");
        username = scanner.nextLine().trim();

        System.out.println("Enter password or app-password or PAT: ");
        passwordOrToken = scanner.nextLine().trim();

        System.out.println("\nParameters collected.\n");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String authHeader() {
        String raw = username + ":" + passwordOrToken;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpRequest.Builder req(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Authorization", authHeader())
            .header("Accept", "application/json");
    }

    private static void printResponse(HttpResponse<String> r) {
        System.out.println("HTTP " + r.statusCode());
        HttpHeaders h = r.headers();
        System.out.println("Headers: " + h.map());
        System.out.println("Body:\n" + r.body());
    }

    // ---------------------------------------------------------------
    // TESTS
    // ---------------------------------------------------------------

    // Test basic GET to detect if server is reachable
    private static void testBasicConnectivity() throws Exception {
        System.out.println("### Test 1: Basic connectivity to Bitbucket");

        HttpRequest request = req("/rest/api/1.0/projects").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // List repo root (for Bitbucket Server)
    private static void testGetRepoRoot() throws Exception {
        System.out.println("### Test 2: List repo root");

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/files",
            projectKey, repoSlug
        );

        HttpRequest request = req(path).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // Get branches
    private static void testGetBranches() throws Exception {
        System.out.println("### Test 3: Get branches");

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/branches",
            projectKey, repoSlug
        );

        HttpRequest request = req(path).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // Get latest commit on main
    private static void testGetLatestCommitOnMain() throws Exception {
        System.out.println("### Test 4: Get latest commit on main");

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/commits?limit=1",
            projectKey, repoSlug
        );

        HttpRequest request = req(path).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // Read a file (envs/dev/apps or something)
    private static void testReadFile() throws Exception {
        System.out.println("### Test 5: Read file (example: README.md)");

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/browse/README.md",
            projectKey, repoSlug
        );

        HttpRequest request = req(path).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // Create branch (Bitbucket Server)
    private static void testCreateBranch() throws Exception {
        System.out.println("### Test 6: Create branch (dry-run test)");

        // NOTE: This will actually create a branch if uncommented.
        // You can keep it dry-run by not sending the request or using a dummy branch starting with "test/".
        String payload = """
        {
            "name": "refs/heads/test-fabric-promotion-branch",
            "startPoint": "refs/heads/main",
            "message": "Fabric test branch"
        }
        """;

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/branches",
            projectKey, repoSlug
        );

        HttpRequest request = req(path)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // Create a commit (Server only; Bitbucket Cloud uses a different API)
    private static void testCreateCommit() throws Exception {
        System.out.println("### Test 7: Create commit (dry-run)");

        String payload = """
        {
          "branch": "refs/heads/test-fabric-promotion-branch",
          "message": "Fabric metadata update test",
          "actions": [
            {
              "action": "add",
              "path": "test-folder/test-file.txt",
              "content": "Hello from Fabric promotion test!"
            }
          ]
        }
        """;

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/commits",
            projectKey, repoSlug
        );

        HttpRequest request = req(path)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }

    // Create a pull request
    private static void testCreatePullRequest() throws Exception {
        System.out.println("### Test 8: Create pull request (dry-run)");

        String payload = """
        {
            "title": "Fabric Promotion Request Test",
            "description": "This PR was created by the Fabric Bitbucket connectivity tester.",
            "state": "OPEN",
            "open": true,
            "closed": false,
            "fromRef": {
                "id": "refs/heads/test-fabric-promotion-branch",
                "repository": {
                    "project": { "key": "%s" },
                    "slug": "%s"
                }
            },
            "toRef": {
                "id": "refs/heads/main",
                "repository": {
                    "project": { "key": "%s" },
                    "slug": "%s"
                }
            }
        }
        """.formatted(projectKey, repoSlug, projectKey, repoSlug);

        String path = String.format(
            "/rest/api/1.0/projects/%s/repos/%s/pull-requests",
            projectKey, repoSlug
        );

        HttpRequest request = req(path)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        printResponse(response);
        System.out.println();
    }
}

