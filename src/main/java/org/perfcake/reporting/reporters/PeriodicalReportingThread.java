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
import org.perfcake.reporting.ReportsException;
import org.perfcake.reporting.destinations.Destination;

/**
 * Thread used for periodical reporters. This thread has additional features
 * like "stopping" the thread and by design isDaemon()==true.
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 */
public class PeriodicalReportingThread extends Thread {
   protected static final Logger log = Logger.getLogger(PeriodicalReportingThread.class);

   /**
    * When should next tick occur
    */
   private long nextPeriod = -1;

   /**
    * Destination for which this thread is created. Each destination has set
    * whether it is should be appended into it periodically.
    */
   private Destination dest;

   /**
    * If set to true then this threads stops as soon as possible.
    */
   private boolean stop = false;

   public PeriodicalReportingThread(String reporterName, Destination dest) throws ReportsException {
      this.dest = dest;
      dest.setPeriodicalThread(this);
      setDaemon(true);
      if (dest != null) {
         setName("periodical-" + reporterName + "-" + dest.getClass().getSimpleName());
      }
   }

   @Override
   public void run() {
      try {
         nextPeriod = System.currentTimeMillis() + (Math.round(dest.getPeriodicalInterval() * 1000));
         while (true) {
            try {
               long sleepTime = nextPeriod - System.currentTimeMillis();
               if (sleepTime < 100) {
                  log.warn("Sleeptime computed very low, setting to 500 : " + sleepTime);
                  sleepTime = 500;
               }
               Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
               return;
            }
            if (stop) {
               return;
            }

            nextPeriod = System.currentTimeMillis() + (Math.round(dest.getPeriodicalInterval() * 1000));
            long timeBefore = System.currentTimeMillis();
            dest.periodicalTick();
            long tickLength = System.currentTimeMillis() - timeBefore;
            if (tickLength > 2000) {
               log.warn("The tick took very long! [thread name: " + getName() + "] : " + tickLength);
            }
         }
      } catch (ReportsException e1) {
         // TODO Auto-generated catch block
         e1.printStackTrace();
      }
   }

   public void stopThread() {
      stop = true;
      interrupt();
   }
}