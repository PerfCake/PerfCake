/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package org.perfcake.reporting.reporter.filter;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.reporter.AbstractReporter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Filters all recognizable numbers ({@link Double}, {@link Long}, {@link Quantity} with Double and Long)
 * in a {@link Measurement} through specified algorithm.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NumericReportingFilter implements ReportingFilter {

   /**
    * The filter's logger.
    */
   private static final Logger log = LogManager.getLogger(AbstractReporter.class);

   /**
    * Filtering algorithms class to be used.
    */
   final private Class<FilterAlgorithm> filterAlgorithmClass;

   /**
    * Algorithms for individual keys in the measurements map.
    */
   final private Map<String, FilterAlgorithm> algorithms = new ConcurrentHashMap<>();

   /**
    * Creates a reporting filter that will be using the specified filtering algorithm.
    *
    * @param filterAlgorithmClass
    *       The filtering algorithm class to be used.
    */
   public NumericReportingFilter(final Class<FilterAlgorithm> filterAlgorithmClass) {
      this.filterAlgorithmClass = filterAlgorithmClass;
   }

   @Override
   public Optional<Measurement> filter(final Measurement measurement) {
      // these two are a trick to use external variables within a lambda block, booleans would be just fine
      final LongAdder wasRecognizedValue = new LongAdder();
      final LongAdder wasFilterOutput = new LongAdder();

      // prepare a new measurement
      final Measurement newMeasurement = new Measurement(measurement.getPercentage(), measurement.getTime(), measurement.getIteration());

      // inspect all measurement result fields.
      measurement.getAll().forEach((k, v) -> {
         // try to discover numeric value
         final ValueDiscovery vd = ValueDiscovery.unwrap(v);

         if (vd != null) {
            wasRecognizedValue.increment();

            // see if we already have an algorithm
            final FilterAlgorithm alg = algorithms.computeIfAbsent(k, key -> {
               try {
                  return filterAlgorithmClass.newInstance();
               } catch (ReflectiveOperationException e) {
                  log.error("Unable to create results filtering algorithm class: ", e);
                  return null;
               }
            });

            if (alg != null) {
               final Optional<Double> f = alg.filter(vd.getValue());

               if (f.isPresent()) { // is there a filter output to report?
                  wasFilterOutput.increment();
                  newMeasurement.set(k, vd.wrap(f.get()));
               }
            } else { // unable to use the algorithm, just copy
               newMeasurement.set(k, v);
            }
         } else { // unrecognized value, just copy
            newMeasurement.set(k, v);
         }
      });

      // either there was nothing to filter, or the filter returned some values
      if (wasRecognizedValue.longValue() == 0 || wasFilterOutput.longValue() > 0) {
         return Optional.of(newMeasurement);
      } else {
         return Optional.empty();
      }
   }

   /**
    * Tries to discover a double or long value in the provided object. The double or long value can even be wrapped in {@link Quantity}.
    */
   private static class ValueDiscovery {

      private final String unit;
      private final boolean wasDouble;

      private final double value;

      private ValueDiscovery(final double value, final String unit, final boolean wasDouble) {
         this.value = value;
         this.unit = unit;
         this.wasDouble = wasDouble;
      }

      private static ValueDiscovery unwrap(final Object v) {
         boolean wasDouble = true;
         Double d = null;
         Quantity q = null;
         if (v instanceof Double) {
            d = (Double) v;
         } else if (v instanceof Long) {
            wasDouble = false;
            d = ((Long) v).doubleValue();
         } else if (v instanceof Quantity) {
            q = (Quantity) v;
            if (q.getNumber() instanceof Double) {
               d = (Double) q.getNumber();
            } else if (q.getNumber() instanceof Long) {
               wasDouble = false;
               d = ((Long) q.getNumber()).doubleValue();
            }
         }

         if (d != null) {
            return new ValueDiscovery(d, q != null ? q.getUnit() : null, wasDouble);
         } else {
            return null;
         }
      }

      private double getValue() {
         return value;
      }

      private Object wrap(final double newValue) {
         if (unit != null) {
            return wasDouble ? new Quantity<>(newValue, unit) : new Quantity<>(((Double) newValue).longValue(), unit);
         } else {
            return wasDouble ? newValue : ((Double) newValue).longValue();
         }
      }
   }
}
