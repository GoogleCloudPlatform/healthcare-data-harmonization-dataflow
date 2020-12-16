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

import static com.google.cloud.healthcare.etl.model.ErrorEntry.ERROR_ENTRY_TAG;

import com.google.cloud.healthcare.etl.model.ErrorEntry;
import java.util.Collections;
import java.util.List;
import org.apache.beam.sdk.transforms.DoFn;

/**
 * This is a base {@link DoFn} class with error reporting enabled automatically, classes which
 * inherit from this class can choose what kind of errors are recoverable, and thus not crashing the
 * whole pipeline. All exceptions will be logged to a separate dataset.
 */
public abstract class ErrorEnabledDoFn<Input, Output> extends DoFn<Input, Output> {
  @ProcessElement
  public void output(ProcessContext ctx) throws Exception {
    Input input = ctx.element();
    try {
      process(ctx);
    } catch (Exception e) {
      ErrorEntry error =
          ErrorEntry.of(e, getErrorResource(input))
              .setStep(getClass().getSimpleName())
              .setSources(getSources(input));
      ctx.output(ERROR_ENTRY_TAG, error);
      // Re-throw if it is not recoverable.
      if (!reportOnly(e)) {
        throw e;
      }
    }
  }

  /**
   * The main processing logic, the sub-class is expected to implement this method and output the
   * results.
   */
  public abstract void process(ProcessContext ctx) throws Exception;

  /**
   * Defines how to extract the error resource for the ErrorEntry from the input. The default
   * populate is empty, override this method to populate the ErrorEntry errorResource.
   */
  protected String getErrorResource(Input input) {
    return "";
  }

  /**
   * Defines how to extract the error source for the ErrorEntry from the input. The default populate
   * is empty, override this method to populate the ErrorEntry sources.
   */
  protected List<String> getSources(Input input) {
    return Collections.emptyList();
  }

  /**
   * Check whether a {@link Throwable} is recoverable, i.e. the pipeline needs to report the error
   * only, rather than crashing. Sub-classes can override this method if they want to report
   * different errors.
   */
  protected boolean reportOnly(Throwable e) {
    return e.getClass() == RuntimeException.class;
  }
}
