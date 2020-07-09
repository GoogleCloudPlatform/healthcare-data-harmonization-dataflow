package com.google.cloud.healthcare.etl.provider.mapping;

import com.google.cloud.storage.StorageOptions;
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;

/** Factory class for creating providers. */
public class MappingConfigProviderFactory {

  public static MappingConfigProvider createProvider(String input) {
    if (input.startsWith(String.format("%s://", GcsPath.SCHEME))) {
      return new GcsMappingConfigProvider(StorageOptions.getDefaultInstance().getService(), input);
    } else {
      return new LocalMappingConfigProvider(input);
    }
  }
}
