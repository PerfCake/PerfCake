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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import io.vertx.core.MultiMap;

/**
 * Generates a new random UUID and sets it as a message header. Than expects the same header in the response.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class GenerateHeaderCorrelator extends AbstractCorrelator {

   /**
    * Name of the correlation header.
    */
   public static final String CORRELATION_HEADER = "perfcake.correlation.id";

   @Override
   public String getRequestCorrelationId(final Message message, final Properties messageAttributes) {
      final String correlationId = UUID.randomUUID().toString();
      
      message.setHeader(CORRELATION_HEADER, correlationId);
      messageAttributes.setProperty(CORRELATION_HEADER, correlationId);

      return correlationId;
   }

   @Override
   public List<String> getResponseCorrelationIds(final Serializable response, final MultiMap headers) {
      return headers.getAll(CORRELATION_HEADER);
   }
}
