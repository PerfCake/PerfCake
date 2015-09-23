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
package org.perfcake.reporting.destinations;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Appends the measurements to Log4j to category org.perfcake.reporting.destinations.Log4jDestination.
 * Make appropriate configurations to customize its output. You can configure a separate appender only for this category for instance.
 * Logging level can be set via the level attribute.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Log4jDestination implements Destination {

   private static final Logger log = LogManager.getLogger(Log4jDestination.class);

   /**
    * Level at which we should log the measurements.
    */
   private Level level = Level.INFO;

   @Override
   public void open() {
      // nop
   }

   @Override
   public void close() {
      // nop
   }

   /**
    * Reports the measurement using log4j at the configured level.
    */
   @Override
   public void report(final Measurement measurement) throws ReportingException {
      switch (level) {
         case DEBUG:
            if (log.isDebugEnabled()) {
               log.debug(measurement.toString());
            }
            break;
         case ERROR:
            log.error(measurement.toString());
            break;
         case FATAL:
            log.fatal(measurement.toString());
            break;
         case INFO:
            if (log.isInfoEnabled()) {
               log.info(measurement.toString());
            }
            break;
         case TRACE:
            if (log.isTraceEnabled()) {
               log.trace(measurement.toString());
            }
            break;
         case WARN:
            log.warn(measurement.toString());
            break;
      }
   }

   /**
    * Gets the current logging level.
    *
    * @return The current logging level.
    */
   public Level getLevel() {
      return level;
   }

   /**
    * Sets the level at which the destination should log the measurement results.
    *
    * @param level
    *       Level at which the destination logs.
    * @return Instance of this for fluent API.
    */
   public Log4jDestination setLevel(final Level level) {
      this.level = level;
      return this;
   }

   /**
    * Log4j level.
    */
   public enum Level { // Log4j level is not an enum, so this would be hard to handle
      TRACE, DEBUG, INFO, WARN, ERROR, FATAL
   }

}
