package org.pronzato.fabric.test.s3;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Minimal standalone S3 smoke test that lists objects from a configured bucket/prefix.
 *
 * <p>Configuration (env var or -D system property):
 *
 * <ul>
 *   <li>{@code FABRIC_TEST_S3_BUCKET} / {@code fabric.test.s3.bucket} (required)
 *   <li>{@code FABRIC_TEST_S3_REGION} / {@code fabric.test.s3.region} (default {@code us-east-1})
 *   <li>{@code FABRIC_TEST_S3_PREFIX} / {@code fabric.test.s3.prefix} (optional path filter)
 * </ul>
 *
 * <p>The AWS SDK default credentials chain is used (env vars, profile, IMDS, etc).
 */
public final class TestS3 {

  private static final String BUCKET = "your-demo-bucket";
  private static final Region REGION = Region.US_EAST_1;
  private static final String PREFIX = ""; // leave empty for whole bucket

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withLocale(Locale.ROOT);

  private TestS3() {}

  public static void main(String[] args) {
    System.out.printf(
        "[TestS3] Listing up to 50 keys from bucket=%s region=%s prefix=%s%n",
        BUCKET, REGION.id(), PREFIX.isBlank() ? "<none>" : PREFIX);

    try (S3Client s3 =
            S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(REGION)
                .build()) {

      ListObjectsV2Request request =
          ListObjectsV2Request.builder()
              .bucket(BUCKET)
              .prefix(PREFIX.isBlank() ? null : PREFIX)
              .maxKeys(50)
              .build();

      ListObjectsV2Response response = s3.listObjectsV2(request);
      List<S3Object> objects = response.contents();
      if (objects == null || objects.isEmpty()) {
        System.out.println("[TestS3] No objects returned for the given prefix.");
        return;
      }

      System.out.printf("[TestS3] Retrieved %d object(s)%n", objects.size());
      for (S3Object object : objects) {
        String key = object.key();
        long size = object.size();
        String lastModified =
            object.lastModified() != null ? DATE_FMT.format(object.lastModified()) : "unknown";
        System.out.printf(
            "  %s | size=%d bytes | lastModified=%s%n", key, size, lastModified);
      }
      if (response.isTruncated()) {
        System.out.println("[TestS3] Results truncated; request NextContinuationToken to continue.");
      }
    } catch (Exception ex) {
      System.err.println("[TestS3] Failed to list S3 objects: " + ex.getMessage());
      ex.printStackTrace(System.err);
      System.exit(1);
    }
  }
}
