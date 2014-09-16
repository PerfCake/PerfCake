package org.perfcake.reporting.reporters;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;

/**
 * Simple reporter that is able to collect and report the statistics about the
 * the default value.
 */
public class ValueStatsReporter extends StatsReporter {
   @Override
   protected Double computeResult(MeasurementUnit mu) {
      return (Double) mu.getResult(Measurement.DEFAULT_RESULT);
   }
}
