/**
 * 
 */
package org.perfcake.nreporting.destinations;

import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.ReportingException;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class ConsoleDestination implements Destination {

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
      StringBuffer sb = new StringBuffer();
      // sb.append
   }

}
