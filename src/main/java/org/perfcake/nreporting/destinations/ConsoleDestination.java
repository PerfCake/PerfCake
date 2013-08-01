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
package org.perfcake.nreporting.destinations;

import org.apache.log4j.Logger;
import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.ReportingException;

/**
 * TODO Should report to Console -- i.e. standard output
 * 
 * The destination that appends the measurements into the console
 * via Log4j's INFO channel.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class ConsoleDestination implements Destination {
   /**
    * The destination's logger.
    */
   private static final Logger log = Logger.getLogger(ConsoleDestination.class);

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      // nop
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.nreporting.destinations.Destination#report(org.perfcake.nreporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      if (log.isInfoEnabled()) {
         log.info(m.toString());
      }
   }

}
