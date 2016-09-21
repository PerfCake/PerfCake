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
package org.perfcake.message.correlator;

import org.perfcake.message.Message;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import io.vertx.core.MultiMap;

/**
 * Test correlator that does not care about the messages and simply returns and incrementing number.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DummyCorrelator extends AbstractCorrelator {

   private AtomicLong counter = new AtomicLong(0);

   @Override
   public String getRequestCorrelationId(final Message message, final Properties messageAttributes) {
      return String.valueOf(counter.getAndIncrement());
   }

   @Override
   public List<String> getResponseCorrelationIds(final Serializable response, final MultiMap headers) {
      return Collections.singletonList(response.toString());
   }

   public long getCounter() {
      return counter.get();
   }
}
