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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class AbstractReportingFilter implements ReportingFilter {

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

   public AbstractReportingFilter(final Class<FilterAlgorithm> filterAlgorithmClass) {
      this.filterAlgorithmClass = filterAlgorithmClass;
   }

   @Override
   public Optional<Measurement> filter(final Measurement measurement) {
      measurement.getAll().forEach((k, v) -> {

         // try to extract double value
         Double d = null;
         Quantity q = null;
         if (v instanceof Double) {
            d = (Double) v;
         } else if (v instanceof Long) {
            d = ((Long) v).doubleValue();
         } else if (v instanceof Quantity) {
            q = (Quantity) v;
            if (q.getNumber() instanceof Double) {
               d = (Double) q.getNumber();
            } else if (q.getNumber() instanceof Long) {
               d = ((Long) q.getNumber()).doubleValue();
            }
         }

         if (d != null) {
            final FilterAlgorithm alg = algorithms.computeIfAbsent(k, key -> {
               try {
                  return filterAlgorithmClass.newInstance();
               } catch (ReflectiveOperationException e) {
                  log.error("Unable to create results filtering algorithm class: ", e);
                  return null;
               }
            });

            if (alg != null) {
               final Optional<Double> f = alg.filter(d);
            }
         }
      });
      return null;
   }
}
