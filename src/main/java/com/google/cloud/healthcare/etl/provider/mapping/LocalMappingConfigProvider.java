package com.google.cloud.healthcare.etl.provider.mapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Reads the mapping configurations out of a local file.
 */
public class LocalMappingConfigProvider implements MappingConfigProvider {

  private final String path;

  public LocalMappingConfigProvider(String path) {
    this.path = path;
  }

  @Override
  public byte[] getMappingConfig(boolean force) throws IOException {
    return Files.readAllBytes(Paths.get(path));
  }
}
