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

/**
 * The destination that appends the measurements to stdout.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ConsoleDestination implements Destination {

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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.reporting.destinations.Destination#report(org.perfcake.reporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      System.out.println(m.toString());
   }

}
