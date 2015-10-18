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
package org.perfcake.reporting;

import org.perfcake.util.Utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Measurement is a product of {@link org.perfcake.reporting.reporters.Reporter}.
 * It is typically a combination of multiple {@link MeasurementUnit Measuremen Units}.
 * The way they are combined is the matter of a particular {@link org.perfcake.reporting.reporters.Reporter}
 * implementation.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Measurement {

   /**
    * The default result name.
    */
   public static final String DEFAULT_RESULT = "Result";

   /**
    * The last progress percentage for what the measurement is valid.
    */
   private final long percentage;

   /**
    * The last timestamp for what the measurement is valid.
    */
   private final long time;

   /**
    * The last iteration for what the measurement is valid.
    */
   private final long iteration;

   /**
    * The map containing the named results.
    */
   private final Map<String, Object> results = new LinkedHashMap<>();

   /**
    * Creates a new instance of Measurement.
    *
    * @param percentage
    *       The current progress of the scenario execution in percents.
    * @param time
    *       The current time of the scenario execution.
    * @param iteration
    *       The current iteration of the scenario execution.
    */
   public Measurement(final long percentage, final long time, final long iteration) {
      super();
      this.percentage = percentage;
      this.time = time;
      this.iteration = iteration;
   }

   /**
    * Gets the percentage value of progress of the scenario execution.
    *
    * @return The percentage progress of the execution.
    */
   public long getPercentage() {
      return percentage;
   }

   /**
    * Gets the time of the scenario execution in milliseconds.
    *
    * @return The time of the execution.
    */
   public long getTime() {
      return time;
   }

   /**
    * Gets the number of iteration to which the {@link org.perfcake.reporting.Measurement} is related.
    *
    * @return The iteration of the execution.
    */
   public long getIteration() {
      return iteration;
   }

   /**
    * Gets the map with the measured results.
    *
    * @return The measured results.
    */
   public Map<String, Object> getAll() {
      return results;
   }

   /**
    * Gets a default result from the result map. The default value is placed in the result map
    * under {@link org.perfcake.reporting.Measurement#DEFAULT_RESULT} key.
    *
    * @return The default result.
    */
   public Object get() {
      return get(DEFAULT_RESULT);
   }

   /**
    * Gets the result with the given name.
    *
    * @param name
    *       The name of the result to get.
    * @return The result value.
    */
   public Object get(final String name) {
      return results.get(name);
   }

   /**
    * Puts the <code>result</code> in the result map with a given <code>name</code> used as a key.
    *
    * @param name
    *       The name of the result.
    * @param result
    *       The result value.
    */
   public void set(final String name, final Object result) {
      results.put(name, result);
   }

   /**
    * Puts the default result in the result map. The default value is placed in the result map
    * under {@link org.perfcake.reporting.Measurement#DEFAULT_RESULT} key.
    *
    * @param result
    *       The default result value.
    */
   public void set(final Object result) {
      results.put(DEFAULT_RESULT, result);
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append("[");
      sb.append(Utils.timeToHMS(time));
      sb.append("][");
      sb.append(iteration + 1); // first iteration index is 0
      sb.append(" iterations][");
      sb.append(percentage);
      sb.append("%]");
      final Object defaultResult = get();
      if (defaultResult != null) {
         sb.append(" [");
         sb.append(get());
         sb.append("]");
      }
      for (final Entry<String, Object> entry : results.entrySet()) {
         if (!entry.getKey().equals(DEFAULT_RESULT)) {
            sb.append(" [");
            sb.append(entry.getKey());
            sb.append(" => ");
            sb.append(entry.getValue());
            sb.append("]");
         }
      }
      return sb.toString();
   }
}
