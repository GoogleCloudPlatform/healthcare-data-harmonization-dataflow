package com.google.cloud.healthcare.etl.provider.mapping;

import java.io.IOException;

/**
 * MappingConfigProvider defines the interface for loading mapping configurations in different
 * format.
 */
public interface MappingConfigProvider {

  /**
   * Returns the mapping configurations as a byte array.
   *
   * @param force forces a read if true, otherwise a cached version may be returned.
   * @return the byte representation of the mapping configurations.
   */
  byte[] getMappingConfig(boolean force) throws IOException;
}
