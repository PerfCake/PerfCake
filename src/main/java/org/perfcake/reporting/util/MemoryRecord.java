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

package org.perfcake.reporting.util;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class MemoryRecord {
   private long timestamp;

   private long free;

   public long getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   private long total;

   private long max;

   public MemoryRecord(long free, long total, long max) {
      this.timestamp = System.currentTimeMillis();
      this.free = free;
      this.total = total;
      this.max = max;
   }

   public MemoryRecord(long timestamp, long free, long total, long max) {
      this.timestamp = timestamp;
      this.free = free;
      this.total = total;
      this.max = max;
   }

   public long getFree() {
      return free;
   }

   public void setFree(long free) {
      this.free = free;
   }

   public long getMax() {
      return max;
   }

   public void setMax(long max) {
      this.max = max;
   }

   public long getTotal() {
      return total;
   }

   public void setTotal(long total) {
      this.total = total;
   }
}
