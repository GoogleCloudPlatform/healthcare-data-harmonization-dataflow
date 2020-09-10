package com.google.cloud.healthcare.etl.runner;

import com.google.cloud.healthcare.etl.model.ErrorEntry;
import com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.healthcare.HealthcareIOError;
import org.apache.beam.sdk.io.gcp.healthcare.HealthcareIOErrorToTableRow;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

/** A helper class for writing error entries. */
public class ErrorWriter {

  private static final String WRITE_ERROR_STEP = "WriteErrors";
  private static final String SERIALIZE_ERROR_STEP = "SerializeErrors";

  public static void writeErrorEntriesToFile(PCollection<ErrorEntry> errors,
      ValueProvider<String> target, String step) {
    errors
        .apply(SERIALIZE_ERROR_STEP,
            MapElements
                .into(TypeDescriptors.strings())
                .via(e -> ErrorEntryConverter.toTableRow(e).toString()))
        .apply(step, TextIO.write().to(target));
  }

  public static void writeErrorEntriesToFile(PCollection<ErrorEntry> errors, String target,
      String step) {
    writeErrorEntriesToFile(errors, StaticValueProvider.of(target), step);
  }

  public static <T> void writeHealthcareErrorsToFile(PCollection<HealthcareIOError<T>> errors,
      ValueProvider<String> target, String step) {
    HealthcareIOErrorToTableRow<T> errorConverter = new HealthcareIOErrorToTableRow<>();
    errors
        .apply(SERIALIZE_ERROR_STEP,
            MapElements
                .into(TypeDescriptors.strings())
                .via(e -> errorConverter.apply(e).toString()))
        .apply(step, TextIO.write().to(target));
  }

  public static <T> void writeHealthcareErrorsToFile(PCollection<HealthcareIOError<T>> errors,
      String target, String step) {
      writeHealthcareErrorsToFile(errors, StaticValueProvider.of(target), step);
  }

  public static void writeErrorEntriesToFile(PCollection<ErrorEntry> errors, String target) {
    writeErrorEntriesToFile(errors, target, WRITE_ERROR_STEP);
  }

  public static <T> void writeHealthcareErrorsToFile(PCollection<HealthcareIOError<T>> errors,
      String target) {
    writeHealthcareErrorsToFile(errors, target, WRITE_ERROR_STEP);
  }

  public static <T> void writeHealthcareErrorsToFile(PCollection<HealthcareIOError<T>> errors,
      ValueProvider<String> target) {
    writeHealthcareErrorsToFile(errors, target, WRITE_ERROR_STEP);
  }

}
