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
package org.perfcake.reporting.destination;

import org.perfcake.reporting.reporter.Reporter;

/**
 * Common ancestor simplifying Destination development. Just stores the parent reporter for later use.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractDestination implements Destination {

   private Reporter parentReporter;

   @Override
   public void open(final Reporter parentReporter) {
      this.parentReporter = parentReporter;
      open();
   }

   /**
    * Opens the destination for reporting.
    */
   public abstract void open();

   /**
    * Gets the parent reporter that opened this destination.
    * @return The parent reporter that opened this destination.
    */
   public Reporter getParentReporter() {
      return parentReporter;
   }
}
