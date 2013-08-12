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
package org.perfcake;

import org.apache.log4j.Logger;
import org.perfcake.util.Utils;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class ScenarioExecution {

   public static final Logger log = Logger.getLogger(ScenarioExecution.class);

   public static void main(final String[] args) {

      log.info("=== Welcome to PerfCake ===");

      Scenario scenario = null;

      try {
         scenario = new Scenario(Utils.getProperty("scenario"));
      } catch (PerfCakeException e) {
         log.fatal("Cannot parse scenario: ", e);
         return;
      }

      try {
         scenario.init();
         scenario.run();
      } catch (PerfCakeException e) {
         log.fatal("Error running scenario: ", e);
      } finally {
         try {
            scenario.close();
         } catch (PerfCakeException e) {
            log.fatal("Scenario did not finish properly: ", e);
         }
      }
   }

}
