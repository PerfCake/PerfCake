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

import org.perfcake.message.correlator.Correlator;
import org.perfcake.util.StringTemplate;

/**
 * Default implementation of basic receiver methods. We do not want them as default implementations
 * in the interface because we want protected visibility of the fields.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractReceiver implements Receiver {

   /**
    * The number of threads to be started.
    */
   protected int threads;

   /**
    * The correlator to register responses.
    */
   protected Correlator correlator;

   /**
    * The source from where to receive the messages.
    */
   private StringTemplate source = new StringTemplate("");

   @Override
   public void setThreads(final int threadCount) {
      threads = threadCount;
   }

   @Override
   public void setCorrelator(final Correlator correlator) {
      this.correlator = correlator;
   }

   @Override
   public final String getSource() {
      return source.toString();
   }

   @Override
   public final Receiver setSource(final String source) {
      this.source = new StringTemplate(source);
      return this;
   }
}
