package com.google.cloud.healthcare.etl.provider.mapping;

import com.google.cloud.storage.StorageOptions;

/** Factory class for creating providers. */
public class MappingConfigProviderFactory {

  public static MappingConfigProvider createProvider(String input) {
    if (input.startsWith("gs://")) {
      return new GcsMappingConfigProvider(StorageOptions.getDefaultInstance().getService(), input);
    } else {
      return new LocalMappingConfigProvider(input);
    }
  }
}
