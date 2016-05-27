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

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DataBuffer {

   private final List<String> attributes;

   private List<String> realAttributes = new LinkedList<>();

   private List<Measurement> data = new ArrayList<>();

   public DataBuffer(final List<String> attributes) {
      this.attributes = attributes;
   }

   public void record(final Measurement measurement) {
      data.add(measurement);

      measurement.getAll().forEach((key, value) -> {
         if (attributes.contains(key) && !realAttributes.contains(key)) {
            realAttributes.add(key);
         }

         attributes.forEach(attr -> {
            if (attr.endsWith("*")) {
               if (key.startsWith(attr.substring(0, attr.length() - 1))) {
                  if (!realAttributes.contains(key)) {
                     realAttributes.add(key);
                  }
               }
            }
         });
      });
   }

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

   public List<String> getAttributes() {
      return realAttributes;
   }
}
