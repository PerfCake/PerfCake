/**
 * 
 */
package org.perfcake.nreporting.destinations;

import org.apache.log4j.Logger;
import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.ReportingException;

/**
 * The destination that appends the measurements into the console
 * via Log4j's INFO channel.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class ConsoleDestination implements Destination {
   /**
    * The destination's logger.
    */
   private static final Logger log = Logger.getLogger(ConsoleDestination.class);

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#report(org.perfcake.nreporting.Measurement)
    */
   @Override
   public void report(Measurement m) throws ReportingException {
      if (log.isInfoEnabled()) {
         log.info(m.toString());
      }
   }

}
