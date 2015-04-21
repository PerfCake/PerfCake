/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.destinations;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A destination for testing purposes.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DummyDestination implements Destination {

   /**
    * Interface for inserting a test assert into the report method of this destination.
    * This is used for test purposes.
    */
   public static interface ReportAssert {
      public void report(Measurement m);
   }

   private String property = null;
   private String property2 = null;

   private String lastMethod = null;
   private Measurement lastMeasurement = null;
   private PeriodType lastType = null;
   private ReportAssert reportAssert = null;

   @Override
   public void open() {
      lastMethod = "open";
   }

   @Override
   public void close() {
      lastMethod = "close";
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      lastMethod = "report";
      lastMeasurement = measurement;
      if (reportAssert != null) {
         reportAssert.report(measurement);
      }
      System.out.println(measurement.toString());
      try {
         throw new Throwable("BAFF");
      } catch (final Throwable t) {
         final StringWriter sw = new StringWriter();
         t.printStackTrace(new PrintWriter(sw));
         if (sw.toString().contains("reportIterations")) {
            lastType = PeriodType.ITERATION;
         } else if (sw.toString().contains("reportPercentage")) {
            lastType = PeriodType.PERCENTAGE;
         } else if (sw.toString().contains("ReportManager$1.run")) {
            lastType = PeriodType.TIME;
         } else {
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
    *       The property value to set.
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
    *       The property2 value to set.
    */
   public void setProperty2(final String property2) {
      this.property2 = property2;
   }

   /**
    * Get the last method called on this object.
    *
    * @return The last method name.
    */
   public String getLastMethod() {
      return lastMethod;
   }

   /**
    * Gets the last measurement seen by this destination.
    *
    * @return The last measurement observed.
    */
   public Measurement getLastMeasurement() {
      return lastMeasurement;
   }

   /**
    * Gets the last type of report period observed by this destination.
    *
    * @return The last type of report period observed.
    */
   public PeriodType getLastType() {
      return lastType;
   }

   /**
    * Gets the report assert registered.
    *
    * @return The report assert set to this destionation.
    */
   public ReportAssert getReportAssert() {
      return reportAssert;
   }

   /**
    * Sets a new report assert to this destination.
    *
    * @param reportAssert
    *       The report assert to be registered.
    */
   public void setReportAssert(final ReportAssert reportAssert) {
      this.reportAssert = reportAssert;
   }

}
