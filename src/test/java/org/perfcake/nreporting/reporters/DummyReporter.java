package org.perfcake.nreporting.reporters;

import org.apache.log4j.Logger;
import org.perfcake.nreporting.MeasurementUnit;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.nreporting.destinations.Destination;
import org.perfcake.util.PeriodType;

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
   protected void doReport(MeasurementUnit mu) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Reporting " + mu.toString());
      }
   }

   @Override
   protected void doPublishResult(PeriodType periodType, Destination d) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Publishing results...");
      }
   }
}
