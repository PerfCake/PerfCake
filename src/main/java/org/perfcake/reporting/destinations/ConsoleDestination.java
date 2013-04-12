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

package org.perfcake.reporting.destinations;

import org.apache.log4j.Logger;
import org.perfcake.reporting.Measurement;

/**
 * <p>
 * No settings for this destination yet. It just appends using log4j.
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class ConsoleDestination extends Destination {
   private static final Logger log = Logger.getLogger(ConsoleDestination.class);

   @Override
   public void loadSpecificConfigValues() {
   }

   @Override
   public synchronized void send() {
      while (!messageQueue.isEmpty()) {
         Measurement m = messageQueue.peek();
         log.info(m);
         messageQueue.poll();
      }
   }

   public void outputCustom(String s) {
      log.info(s);
   }
}
