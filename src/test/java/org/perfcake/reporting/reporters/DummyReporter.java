package org.perfcake.reporting.reporters;

import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.AbstractReporter;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class DummyReporter extends AbstractReporter {

   private String lastMethod = null;

   /**
    * The reporter's loger.
    */
   private static final Logger log = Logger.getLogger(DummyReporter.class);

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Reporting " + mu.toString());
      }
      lastMethod = "doReport";
   }

   @Override
   protected void doPublishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Publishing results...");
      }
      lastMethod = "doPublishResult";
   }

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      lastMethod = "getAccumulator";
      return new LastValueAccumulator();
   }

   @Override
   protected void doReset() {
      // nothing needed
      lastMethod = "doReset";
   }

   public String getLastMethod() {
      return lastMethod;
   }

}
