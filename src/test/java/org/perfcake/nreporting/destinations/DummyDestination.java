package org.perfcake.nreporting.destinations;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.perfcake.common.PeriodType;
import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.ReportingException;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class DummyDestination implements Destination {

   private String property = null;
   private String property2 = null;

   private String lastMethod = null;
   private Measurement lastMeasurement = null;
   private PeriodType lastType = null;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      lastMethod = "open";
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      lastMethod = "close";
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#report(org.perfcake.nreporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      lastMethod = "report";
      lastMeasurement = m;
      System.out.println(m.toString());
      try {
         throw new Throwable("BAFF");
      } catch (Throwable t) {
         StringWriter sw = new StringWriter();
         t.printStackTrace(new PrintWriter(sw));
         if (sw.toString().contains("reportIterations")) {
            lastType = PeriodType.ITERATION;
         } else if (sw.toString().contains("reportPercentage")) {
            lastType = PeriodType.PERCENTAGE;
         } else if (sw.toString().contains("AbstractReporter$1.run")) {
            lastType = PeriodType.TIME;
         } else {
            t.printStackTrace();
            lastType = null;
         }
      }
   }

   /**
    * Used to read the value of property.
    * 
    * @return The property value.
    */
   public String getProperty() {
      return property;
   }

   /**
    * Used to set the value of property.
    * 
    * @param property
    *           The property value to set.
    */
   public void setProperty(final String property) {
      this.property = property;
   }

   /**
    * Used to read the value of property2.
    * 
    * @return The property2 value.
    */
   public String getProperty2() {
      return property2;
   }

   /**
    * Used to set the value of property2.
    * 
    * @param property2
    *           The property2 value to set.
    */
   public void setProperty2(final String property2) {
      this.property2 = property2;
   }

   public String getLastMethod() {
      return lastMethod;
   }

   public Measurement getLastMeasurement() {
      return lastMeasurement;
   }

   public PeriodType getLastType() {
      return lastType;
   }

}
