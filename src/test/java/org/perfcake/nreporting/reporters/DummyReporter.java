package org.perfcake.nreporting.reporters;

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.nreporting.MeasurementUnit;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.nreporting.destinations.Destination;
import org.perfcake.nreporting.reporters.accumulators.Accumulator;
import org.perfcake.nreporting.reporters.accumulators.AvgAccumulator;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class DummyReporter extends AbstractReporter {

   /**
    * The reporter's loger.
    */
   private static final Logger log = Logger.getLogger(DummyReporter.class);

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Reporting " + mu.toString());
      }
   }

   @Override
   protected void doPublishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Publishing results...");
      }
   }

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      return new AvgAccumulator();
   }

   @Override
   protected void doReset() {
      // nothing needed
   }
}
