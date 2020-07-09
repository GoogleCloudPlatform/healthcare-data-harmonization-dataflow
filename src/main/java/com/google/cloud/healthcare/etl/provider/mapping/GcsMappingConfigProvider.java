package com.google.cloud.healthcare.etl.provider.mapping;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;

/**
 * This class provides the mapping configurations from files. This can be local files on the disk,
 * or files on a blob system, e.g. GCS. Only local files and GCS files are supported at this moment.
 */
public class GcsMappingConfigProvider implements MappingConfigProvider {
  private final Storage gcsClient;
  private final GcsPath gcsPath;
  private byte[] config;

  public GcsMappingConfigProvider(Storage storage, String path) {
    this.gcsClient = storage;
    this.gcsPath = GcsPath.fromUri(path);
  }

  @Override
  public byte[] getMappingConfig(boolean force) throws IOException {
    if (force || config == null) {
      Blob blob = gcsClient.get(BlobId.of(gcsPath.getBucket(), gcsPath.getObject()));
      config = blob.getContent();
    }
    return config;
  }
}
