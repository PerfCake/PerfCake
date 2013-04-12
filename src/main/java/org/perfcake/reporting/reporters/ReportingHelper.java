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

package org.perfcake.reporting.reporters;

public class ReportingHelper {
   /**
    * Human readable representation of how much time passed after start of the
    * test.
    */
   public static String getTimeOfTest(long start, long currentTime) {
      long t = (currentTime - start) / 1000;
      int hours = (int) (t / 3600);
      t = t % 3600;
      int mins = (int) (t / 60);
      int sec = (int) (t % 60);
      return String.format("%02d:%02d:%02d", hours, mins, sec);
   }
}
