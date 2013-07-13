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

import org.apache.log4j.Logger;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.destinations.Destination;

/**
 * <p>
 * This reporter reports custom data provided by programmer through Measurement.
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class CustomReporter extends Reporter {
   private static final Logger log = Logger.getLogger(CustomReporter.class);

   @Override
   public void loadConfigVals() {
   }

   public void report(Measurement measurement) throws ReportsException {
      measurement.setLabelType(getLabelType(measurement.getLabelType()));
      measurement.setLabel(getLabel(measurement.getLabel()));

      for (Destination dest : destinations) {
         dest.addMessageToSendQueue(measurement);
      }
   }

   @Override
   public void testStarted() {
   }

   @Override
   public void testEnded() throws ReportsException {
      for (Destination dest : destinations) {
         dest.send();
      }
   }

   @Override
   public void periodicalTick(Destination dest) throws ReportsException {
      dest.send();
   }
}
