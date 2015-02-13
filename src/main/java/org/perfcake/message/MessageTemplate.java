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
package org.perfcake.message;

import org.perfcake.util.StringTemplate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

/**
 * TODO logging and javadoc
 *
 * @author <a href="mailto:lucie.fabrikova@gmail.com">Lucie Fabriková</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MessageTemplate implements Serializable {
   private static final long serialVersionUID = 6172258079690233417L;

   private transient Logger log = LogManager.getLogger(MessageTemplate.class);

   private final Message message;
   private final long multiplicity;
   private final List<String> validatorIds;
   private final boolean isStringMessage;
   private transient StringTemplate template;

   public MessageTemplate(final Message message, final long multiplicity, final List<String> validatorIds) {
      this.message = message;
      this.isStringMessage = message.getPayload() instanceof String;
      if (isStringMessage) {
         prepareTemplate();
      }
      this.multiplicity = multiplicity;
      this.validatorIds = validatorIds;
   }

   public Message getMessage() {
      return message;
   }

   public Message getFilteredMessage(final Properties props) {
      if (isStringMessage && template != null && template.hasPlaceholders()) {
         final Message m = MessageFactory.getMessage();

         m.setPayload(template.toString(props));
         m.setHeaders(message.getHeaders());
         m.setProperties(message.getProperties());

         return m;
      } else {
         return message;
      }
   }

   private void prepareTemplate() {
      // find out if there are any attributes in the text message to be replaced
      final StringTemplate tmpTemplate = new StringTemplate((String) message.getPayload());

      if (tmpTemplate.hasPlaceholders()) {
         if (log.isDebugEnabled()) {
            log.debug("Created matching pattern for the message payload with properties.");
         }
         this.template = tmpTemplate;
      } else {
         // return the rendered template back, it might not have any placeholders now, but there could have been some math replacements etc.
         message.setPayload(tmpTemplate.toString());
      }
   }

   public Long getMultiplicity() {
      return multiplicity;
   }

   public List<String> getValidatorIds() {
      return validatorIds;
   }

   // fill in the transient field
   private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
      stream.defaultReadObject();
      if (isStringMessage) {
         prepareTemplate();
      }
   }
}
