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
 * A record that is valid for a particular time stamp.
 * The record has a time stamp and a value.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class TimestampedRecord<T extends Number> {

   /**
    * Time stamp.
    */
   private long timestamp;

   /**
    * The value.
    */
   private T value;

   /**
    * Creates a new record.
    *
    * @param timestamp
    *       The time stamp of the record.
    * @param value
    *       The value of the record.
    */
   public TimestampedRecord(long timestamp, T value) {
      super();
      this.timestamp = timestamp;
      this.value = value;
   }

   /**
    * Used to read the value of timestamp.
    *
    * @return The timestamp value.
    */
   public long getTimestamp() {
      return timestamp;
   }

   /**
    * Used to set the value of timestamp.
    *
    * @param timestamp
    *       The timestamp value to set.
    */
   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   /**
    * Used to read the value.
    *
    * @return The value.
    */
   public T getValue() {
      return value;
   }

   /**
    * Used to set the value of value.
    *
    * @param value
    *       The value to set.
    */
   public void setValue(T value) {
      this.value = value;
   }

}
