/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.reporting.reporter;

import org.perfcake.PerfCakeConst;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.properties.MandatoryProperty;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Counts of occurrences of individual values in the given message attribute.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ClassifyingReporter extends AbstractReporter {

   /**
    * Message attribute the values of which should be classified and counted.
    */
   @MandatoryProperty
   private String attribute = null;

   /**
    * Prefix of the individual classes.
    */
   private String prefix = "class_";

   private Map<String, LongAdder> classes = new ConcurrentHashMap<>();

   @Override
   protected void doReset() {
      classes = new ConcurrentHashMap<>();
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      final Properties messageAttributes = (Properties) measurementUnit.getResult(PerfCakeConst.ATTRIBUTES_TAG);

      if (messageAttributes != null && attribute != null) {
         final String attributeClass = messageAttributes.getProperty(attribute);
         if (attributeClass != null) {
            classes.computeIfAbsent(attributeClass, k -> new LongAdder());
            classes.get(attributeClass).increment();
         }
      }
   }

   @Override
   public Measurement computeMeasurement(final PeriodType periodType) throws ReportingException {
      final Measurement m = newMeasurement();
      publishAccumulatedResult(m);

      classes.forEach((key, value) -> m.set(prefix + key, value.longValue()));

      return m;
   }

   /**
    * Gets the name of the message attribute that is being classified and its values counted.
    *
    * @return The name of the message attribute that is being classified and its values counted.
    */
   public String getAttribute() {
      return attribute;
   }

   /**
    * Sets the name of the attribute the values of which are classified and counted.
    *
    * @param attribute
    *       The name of the attribute the values of which are classified and counted.
    * @return Instance of this to support fluent API.
    */
   public ClassifyingReporter setAttribute(final String attribute) {
      this.attribute = attribute;
      return this;
   }

   /**
    * Gets the prefix of the result keys for each value class.
    *
    * @return The prefix of the result keys for each value class.
    */
   public String getPrefix() {
      return prefix;
   }

   /**
    * Sets the prefix of the result keys for each value class.
    *
    * @param prefix
    *       The prefix of the result keys for each value class.
    * @return Instance of this to support fluent API.
    */
   public ClassifyingReporter setPrefix(final String prefix) {
      this.prefix = prefix;
      return this;
   }
}
