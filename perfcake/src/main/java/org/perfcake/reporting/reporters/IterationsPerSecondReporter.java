package org.perfcake.reporting.reporters;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;

public class IterationsPerSecondReporter extends AbstractReporter {
   @Override
   protected void doReset() {
      // nothing is needed here
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      // nothing is needed here
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      final Measurement m = newMeasurement();
      m.set(Measurement.DEFAULT_RESULT, new Quantity<>(1000.0 * runInfo.getIteration() / runInfo.getRunTime(), "iterations/s"));
      destination.report(m);
   }
}
