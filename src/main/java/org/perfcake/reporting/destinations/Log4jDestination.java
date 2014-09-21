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

import org.apache.log4j.Logger;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;

/**
 * The destination that appends the measurements to Log4j to category org.perfcake.reporting.destinations.Log4jDestination.
 * Make appropriate configurations to customize its output. You can configure a separate appender only for this category for instance.
 * Logging level can be set via the level attribute.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Log4jDestination implements Destination {

   private static final Logger log = Logger.getLogger(Log4jDestination.class);

   /**
    * Level at which we should log the measurements
    */
   private Level level = Level.INFO;

   public static enum Level { // Log4j level is not an enum, so this would be hard to handle
      TRACE, DEBUG, INFO, WARN, ERROR, FATAL;
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      // nop
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      // nop
   }

   /**
    * Reports the measurement using log4j at the configured level.s
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      switch (level) {
         case DEBUG:
            if (log.isDebugEnabled()) {
               log.debug(m.toString());
            }
            break;
         case ERROR:
            log.error(m.toString());
            break;
         case FATAL:
            log.fatal(m.toString());
            break;
         case INFO:
            if (log.isInfoEnabled()) {
               log.info(m.toString());
            }
            break;
         case TRACE:
            if (log.isTraceEnabled()) {
               log.trace(m.toString());
            }
            break;
         case WARN:
            log.warn(m.toString());
            break;
      }
   }

   /**
    * Get the current logging level.
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
    *           Level at which the destination logs.
    */
   public Log4jDestination setLevel(final Level level) {
      this.level = level;
      return this;
   }

}
