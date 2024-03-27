package com.google.cloud.healthcare.etl.provider.mapping;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the mapping configurations from files. This can be local files on the disk,
 * or files on a blob system, e.g. GCS. Only local files and GCS files are supported at this moment.
 */
public class GcsMappingConfigProvider implements MappingConfigProvider {
  private final Storage gcsClient;
  private final GcsPath gcsPath;
  private byte[] config;
  private static final Logger LOGGER = LoggerFactory.getLogger(GcsMappingConfigProvider.class);

  public GcsMappingConfigProvider(Storage storage, String path) {
    this.gcsClient = storage;
    this.gcsPath = GcsPath.fromUri(path);
  }

  /**
   * This function downloads the mapping configurations to tmp directory based on the importRoot
   * name provided in params
   */
  @Override
  public byte[] getMappingConfig(boolean force, String importRoot) throws IOException {
    if (force || config == null) {
      if (importRoot.isEmpty()) {
        throw new IOException("Re-run the pipeline with --importRoot argument");
      }
      LOGGER.info("Fetching Whistle configs from" + importRoot);
      Blob blob = gcsClient.get(BlobId.of(gcsPath.getBucket(), gcsPath.getObject()));
      // TODO(b/329285315): Harmonization Dataflow Pipeline gcs error logging
      Page<Blob> mappingBlobs =
          gcsClient.list(gcsPath.getBucket(), Storage.BlobListOption.prefix(importRoot));
      // Lists all the files under prefix importRoot and downloads to tmp dir
      for (Blob mappingBlob : mappingBlobs.iterateAll()) {
        String fileName = mappingBlob.getName();
        // Creates subdirectories as needed to maintain the folder structure for correct imports.
        new File("/tmp/" + fileName).getParentFile().mkdirs();
        mappingBlob.downloadTo(Paths.get("/tmp/" + fileName));
      }
      config = blob.getContent();
    }
    LOGGER.info("Whistle configs fetched successfully");
    return config;
  }
}
