// Copyright 2020 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.healthcare.etl.pipeline;

import com.google.cloud.healthcare.etl.model.mapping.Mappable;
import com.google.cloud.healthcare.etl.model.mapping.MappedFhirMessageWithSourceTime;
import com.google.cloud.healthcare.etl.model.mapping.MappingOutput;
import com.google.cloud.healthcare.etl.provider.mapping.MappingConfigProvider;
import com.google.cloud.healthcare.etl.provider.mapping.MappingConfigProviderFactory;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.FileLoader;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The core function of the mapping pipeline. Input is expected to be a parsed message. At this
 * moment, only higher level language (whistle) is supported.
 */
public class MappingFn<M extends Mappable> extends ErrorEnabledDoFn<M, MappingOutput> {
  // TODO(b/173141038): refactor the class for capturing performance metrics better.
  private static final Logger LOGGER = LoggerFactory.getLogger(MappingFn.class);
  public static final TupleTag<MappingOutput> MAPPING_TAG = new TupleTag<>("mapping");
  protected final Distribution transformMetrics =
      Metrics.distribution(MappingFn.class, "Transform");

  // Ensure the initialization only happens once. Ideally this should be handled by the library.
  private static final AtomicBoolean initialized = new AtomicBoolean();

  private final ValueProvider<String> mappingPath;
  private final ValueProvider<String> mappings;
  private final ValueProvider<String> importRoot;
  private final boolean enablePerformanceMetrics;

  protected Engine engine;

  // The config parameter should be the string representation of the whole mapping config, including
  // harmonization and libraries.
  protected MappingFn(
      ValueProvider<String> mappingPath,
      ValueProvider<String> mappings,
      Boolean enablePerformanceMetrics,
      ValueProvider<String> importRoot) {
    this.mappingPath = mappingPath;
    this.mappings = mappings;
    this.enablePerformanceMetrics = enablePerformanceMetrics;
    this.importRoot = importRoot;
  }

  /**
   * Creates a mapping function with the given mapping path and performance metrics flag.
   *
   * @param mappingPath the path to the main mapping config file.
   * @param enablePerformanceMetrics whether to enable performance metrics.
   * @param importRoot the root folder of the mapping configuration.
   * @return a mapping function.
   */
  public static MappingFn of(
      ValueProvider<String> mappingPath,
      Boolean enablePerformanceMetrics,
      ValueProvider<String> importRoot) {
    return new MappingFn(
        mappingPath, StaticValueProvider.of(""), enablePerformanceMetrics, importRoot);
  }

  /**
   * Creates a mapping function with the given mapping path and performance metrics flag.
   *
   * @param mappingPath the path to the main mapping config file.
   * @param enablePerformanceMetrics whether to enable performance metrics.
   * @param importRoot the root folder of the mapping configuration.
   * @return a mapping function.
   */
  public static MappingFn of(
      String mappingPath, Boolean enablePerformanceMetrics, String importRoot) {
    return of(
        StaticValueProvider.of(mappingPath),
        enablePerformanceMetrics,
        StaticValueProvider.of(importRoot));
  }

  /**
   * Creates a mapping function with the given mapping path and performance metrics flag.
   *
   * @param mappingPath the path to the mapping configuration.
   * @param enablePerformanceMetrics whether to enable performance metrics.
   * @return a mapping function.
   */
  public static MappingFn of(ValueProvider<String> mappingPath, Boolean enablePerformanceMetrics) {
    return new MappingFn(
        mappingPath,
        StaticValueProvider.of(""),
        enablePerformanceMetrics,
        StaticValueProvider.of(""));
  }

  /**
   * Creates a mapping function with the given mapping path and performance metrics flag.
   *
   * @param mappingPath the path to the mapping configuration.
   * @param enablePerformanceMetrics whether to enable performance metrics.
   * @return a mapping function.
   */
  public static MappingFn of(String mappingPath, Boolean enablePerformanceMetrics) {
    return of(StaticValueProvider.of(mappingPath), enablePerformanceMetrics);
  }

  /**
   * Creates a mapping function with the given mapping path, mapping configuration, performance
   * metrics flag and root folder.
   *
   * @param mappingPath the path to the main mapping config file.
   * @param mappings the mapping configuration.
   * @param enablePerformanceMetrics whether to enable performance metrics.
   * @param importRoot the root folder of the mapping configuration.
   * @return a mapping function.
   */
  public static MappingFn of(
      ValueProvider<String> mappingPath,
      ValueProvider<String> mappings,
      Boolean enablePerformanceMetrics,
      ValueProvider<String> importRoot) {
    return new MappingFn(
        mappingPath, mappings, enablePerformanceMetrics, StaticValueProvider.of(importRoot));
  }

  @Setup
  public void initialize() {
    // Make sure the mapping configuration is only initialized once.
    synchronized (initialized) {
      if (!initialized.get()) {
        LOGGER.info("Initializing the mapping configurations.");
        try {
          // Mapping configurations are loaded from the `mappingPath` only if `mappings` is absent.
          String mappingsToUse = mappings.get();
          String importRootToUse = importRoot.get();
          if (Strings.isNullOrEmpty(mappingsToUse)) {
            String mappingConfig = loadMapping(mappingPath.get(), importRootToUse);
            mappingsToUse = mappingPath.get();
          }
          String rootFile =
              mappingsToUse.substring(
                  mappingsToUse.indexOf(importRootToUse), mappingsToUse.length());
          Path mappingPathLocal = FileSystems.getDefault().getPath("/tmp/" + rootFile);
          ImportPath mappingImportPath =
              ImportPath.of(FileLoader.NAME, mappingPathLocal, mappingPathLocal.getParent());

          engine =
              new Engine.Builder(ExternalConfigExtractor.of(mappingImportPath))
                  .initialize()
                  .build();

        } catch (RuntimeException e) {
          LOGGER.error("Unable to initialize mapping configurations.", e);
          throw e; // Fail fast.
        } catch (IOException e) {
          LOGGER.error("Unable to initialize mapping engine.", e);
          throw new RuntimeException(e);
        }
        initialized.set(true);
      }
    }
  }

  private static String loadMapping(String mappingPath, String importRoot) {
    MappingConfigProvider provider = MappingConfigProviderFactory.createProvider(mappingPath);
    try {
      return new String(
          provider.getMappingConfig(/* force= */ true, importRoot), StandardCharsets.UTF_8);
    } catch (IOException | NullPointerException e) {
      throw new RuntimeException("Unable to load mapping configurations.", e);
    }
  }

  // Runs a lambda and collects the metrics.
  protected static <T> T runAndReportMetrics(Distribution metrics, Supplier<T> supplier) {
    Instant start = Instant.now();
    T result = supplier.get();
    metrics.update(Instant.now().toEpochMilli() - start.toEpochMilli());
    return result;
  }

  @Override
  public void process(ProcessContext ctx) {
    M input = ctx.element();
    ctx.output(
        runAndReportMetrics(
            transformMetrics,
            () -> {
              String transformedData = engine.transform(input.getData());
              return (enablePerformanceMetrics)
                  ? new MappedFhirMessageWithSourceTime(
                      transformedData, input.getCreateTime().get())
                  : new MappedFhirMessageWithSourceTime(transformedData);
            }));
  }

  @Override
  protected List<String> getSources(M input) {
    return Collections.singletonList(input.getId());
  }

  /**
   * Multiple tests run from the same jvm could end up reusing the same mappings leading to
   * side-effects. This function is exposed give tests the ability to re-initialize mappings.
   *
   * <p>ONLY FOR TESTS.
   */
  @VisibleForTesting
  public static void resetInitialize() {
    initialized.set(false);
  }
}
