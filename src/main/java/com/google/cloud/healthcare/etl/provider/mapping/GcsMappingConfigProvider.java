package com.google.cloud.healthcare.etl.provider.mapping;

import com.google.cloud.healthcare.etl.pipeline.MappingFn;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;
import java.nio.file.Paths;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.api.gax.paging.Page;



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
    @Override
  public byte[] getMappingConfig(boolean force, String rootFolder) throws IOException {
    if (force || config == null) {
      Blob blob = gcsClient.get(BlobId.of(gcsPath.getBucket(), gcsPath.getObject()));
      Page<Blob> mapping_blobs = gcsClient.list(gcsPath.getBucket(),Storage.BlobListOption.prefix(rootFolder));
      //TODO: add error logging
      for (Blob m_blob : mapping_blobs.iterateAll()) {
        String fileName = m_blob.getName();
        String mapping_path = fileName.substring(0,(fileName.lastIndexOf('/')));
        Path outputDir =
            FileSystems.getDefault().getPath("/tmp/" + mapping_path);
        File outputDirFile = outputDir.toFile();
        if (!outputDirFile.exists()){new File("/tmp/"+mapping_path).mkdirs();}
        if(!((mapping_path+"/").equals(fileName))) {
          m_blob.downloadTo(Paths.get("/tmp/" + fileName));
        }
      }
      config = blob.getContent();
    }
    return config;
  }
}
