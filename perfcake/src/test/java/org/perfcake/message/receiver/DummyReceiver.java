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
package org.perfcake.message.receiver;

import org.perfcake.PerfCakeException;
import org.perfcake.message.correlator.DummyCorrelator;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DummyReceiver extends AbstractReceiver implements Runnable {

   private long messagesToConfirm = 0;

   private Thread thread;

   @Override
   public void start() throws PerfCakeException {
      thread = new Thread(this);
      thread.start();
   }

   @Override
   public void stop() {
      try {
         thread.join();
      } catch (InterruptedException e) {
         // we don't care
      }
   }

   @Override
   public void run() {
      long l = 0;
      if (correlator instanceof DummyCorrelator) {
         while (l < messagesToConfirm) {
            if (((DummyCorrelator) correlator).getCounter() > l) {
               correlator.registerResponse(String.valueOf(l++), null);
            } else {
               try {
                  Thread.sleep(100);
               } catch (InterruptedException e) {
                  // we don't care
               }
            }
         }
      }
   }

   public long getMessagesToConfirm() {
      return messagesToConfirm;
   }

   public void setMessagesToConfirm(final long messagesToConfirm) {
      this.messagesToConfirm = messagesToConfirm;
   }
}
