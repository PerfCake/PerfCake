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
package org.perfcake.common;

/**
 * A recorded number matching with a particular time stamp.
 * The record has a time stamp and a value. Instances are immutable.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class TimestampedRecord<T extends Number> {

   /**
    * Time stamp.
    */
   private final long timestamp;

   /**
    * The value.
    */
   private final T value;

   /**
    * Creates a new record using the provided time stamp and value bound to it.
    *
    * @param timestamp
    *       The time stamp of the record.
    * @param value
    *       The value of the record.
    */
   public TimestampedRecord(final long timestamp, final T value) {
      super();
      this.timestamp = timestamp;
      this.value = value;
   }

   /**
    * Gets the timestamp of the record.
    *
    * @return The timestamp.
    */
   public long getTimestamp() {
      return timestamp;
   }

   /**
    * Gets the value of the record.
    *
    * @return The value.
    */
   public T getValue() {
      return value;
   }

}
