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

import io.vertx.core.MultiMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Extract a prefix from the message and then the same prefix is expected to be in the response.
 * It should be used where messages cannot have headers for instance.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class PrefixCorrelator extends AbstractCorrelator {

   /**
    * A char or a string that indicates the end of thee prefix at the beginning of the message or response.
    */
   public String prefixBoundary = ":";

   @Override
   public String getRequestCorrelationId(final Message message, final Properties messageAttributes) {
      return getPrefix(message.getPayload().toString());
   }

   @Override
   public List<String> getResponseCorrelationIds(final Serializable response, final MultiMap headers) {
      return Arrays.asList(getPrefix(response.toString()));
   }

   /**
    * Extracts the correlation prefix from a string.
    *
    * @param message
    *       A String from the prefix is taken.
    * @return A substring of the message between the beginning and the prefix boundadry.
    */
   private String getPrefix(final String message) {
      return message.substring(0, message.indexOf(getPrefixBoundary()));
   }

   /**
    * Returns the prefix boundary - a char or a string indicating the end of prefix in the message or the response.
    *
    * @return The prefix boundary.
    */
   public String getPrefixBoundary() {
      return prefixBoundary;
   }

   /**
    * Change the prefix boundary - a char or a string indicating the end of prefix in the message or the response.
    *
    * @param prefixBoundary
    *       The prefix boundary.
    * @return An instance of this to support fluent API.
    */
   public PrefixCorrelator setPrefixBoundary(final String prefixBoundary) {
      this.prefixBoundary = prefixBoundary;
      return this;
   }
}
