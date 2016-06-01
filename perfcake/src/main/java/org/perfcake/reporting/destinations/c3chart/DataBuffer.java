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
package org.perfcake.reporting.destinations.c3chart;

import org.perfcake.PerfCakeConst;
import org.perfcake.reporting.Measurement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Buffer for storing measurements. It notices which attributes passed through and creates an ultimate list of them.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DataBuffer {

   private final List<String> attributes;

   private Set<String> realAttributes = Collections.synchronizedSet(new HashSet<>());

   private List<Measurement> data = new ArrayList<>();

   /**
    * Creates an empty buffer with an initial list of attributes to watch for.
    *
    * @param attributes The attributes to watch for, including * expansions.
    */
   public DataBuffer(final List<String> attributes) {
      this.attributes = attributes;
   }

   /**
    * Records the measurement and notices which attributes were present.
    * In case of the warmUp phase, we record the attributes with _warmUp suffix to later separate the two phases.
    * The list of attributes with the _warmUp extension is created in {@link C3ChartHelper}.
    *
    * @param measurement The measurement to be recorded.
    */
   public void record(final Measurement measurement) {
      data.add(measurement);

      boolean isWarmUp = measurement.get(PerfCakeConst.WARM_UP_TAG) != null ? (Boolean) measurement.get(PerfCakeConst.WARM_UP_TAG) : false;

      measurement.getAll().forEach((key, value) -> {
         if (!PerfCakeConst.WARM_UP_TAG.equals(key)) {
            // should we record in the attribute with the _warmUp extension?
            final String warmUpKey = key + (isWarmUp ? "_" + PerfCakeConst.WARM_UP_TAG : "");

            // did we already see the attribute?
            if ((isWarmUp && !realAttributes.contains(warmUpKey)) || (!isWarmUp && !realAttributes.contains(key))) {
               if (attributes.contains(warmUpKey)) { // plain storage, there is no * for this attribute in the list of required attributes
                  realAttributes.add(warmUpKey);
               }
               if (attributes.contains(key)) {
                  realAttributes.add(key);
               } else {
                  attributes.forEach(attr -> { // search the list of required attributes for the names with *
                     if (attr.endsWith("*")) {
                        // no matter the warmUp state, we always record the attributes if there is a plain match, where there is attr*_warmUp, there also is attr*
                        if (key.startsWith(attr.substring(0, attr.length() - 1)) && !PerfCakeConst.WARM_UP_TAG.equals(key)) {
                           realAttributes.add(key);
                        }
                     } else if (attr.endsWith("*_" + PerfCakeConst.WARM_UP_TAG)) {
                        if (key.startsWith(attr.substring(0, attr.length() - 2 - PerfCakeConst.WARM_UP_TAG.length())) && isWarmUp) {
                           realAttributes.add(warmUpKey);
                        }
                     }
                  });
               }
            }
         }
      });
   }

   /**
    * Replays the stored measurements to the given consumer.
    * Makes sure that each measurement contains all the attributes (it adds them with null values if some were missing).
    *
    * @param consumer Consumer of the records. The records are replayed in the original order.
    */
   public void replay(final Consumer<Measurement> consumer) {
      data.forEach(measurement -> {
         realAttributes.forEach(attribute -> {
            if (measurement.get(attribute) == null) {
               measurement.set(attribute, null);
            }
         });

         consumer.accept(measurement);
      });
   }

   /**
    * Get the list of all attributes noticed during recording.
    *
    * @return The list of all attributes noticed during recording.
    */
   public List<String> getAttributes() {
      final List<String> result = realAttributes.stream().collect(Collectors.toList());
      result.sort(String::compareTo);

      return result;
   }
}
