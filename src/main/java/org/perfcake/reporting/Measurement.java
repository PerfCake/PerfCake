/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.perfcake.reporting;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.perfcake.util.Utils;

/**
 * Measurement is a product of {@link org.perfcake.reporting.reporters.Reporter}. It is typically a combination of multiple {@link MeasurementUnit Measuremen Units}. The way they are combined is the matter of a particular Reporter.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
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
   private final Map<String, Object> results = new HashMap<>();

   /**
    * Creates a new instance of Measurement.
    * 
    * @param percentage
    * @param time
    * @param iteration
    */
   public Measurement(final long percentage, final long time, final long iteration) {
      super();
      this.percentage = percentage;
      this.time = time;
      this.iteration = iteration;
   }

   /**
    * Used to read the value of percentage.
    * 
    * @return The percentage.
    */
   public long getPercentage() {
      return percentage;
   }

   /**
    * Used to read the value of time.
    * 
    * @return The time.
    */
   public long getTime() {
      return time;
   }

   /**
    * Used to read the value of iteration.
    * 
    * @return The iteration.
    */
   public long getIteration() {
      return iteration;
   }

   /**
    * Used to read the value of results.
    * 
    * @return The results.
    */
   public Map<String, Object> getAll() {
      return results;
   }

   /**
    * Used to read the value of the default result.
    * 
    * @return The default result.
    */
   public Object get() {
      return get(DEFAULT_RESULT);
   }

   /**
    * @param name
    * @return
    */
   public Object get(final String name) {
      return results.get(name);
   }

   /**
    * 
    * Used to append a named result.
    * 
    * @param name
    *           Name of the result.
    * @param result
    *           The result value.
    */
   public void set(final String name, final Object result) {
      results.put(name, result);
   }

   /**
    * Used to set the default result.
    * 
    * @param result
    *           The default result value.
    */
   public void set(final Object result) {
      results.put(DEFAULT_RESULT, result);
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#toString()
    */
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
