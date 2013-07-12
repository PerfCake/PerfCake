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

package org.perfcake.message;

import java.io.Serializable;
import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

/**
 * 
 * @author Marek Baluch <baluchw@gmail.com>
 */
public class MessageFactory {

   public static Message getMessage() {
      return new Message();
   }

   public static Message getMessage(Serializable payload) {
      return new Message(payload);
   }

   public static Message getMessage(javax.jms.Message jmsMessage) throws JMSException {
      Message perfCakeMessage = getMessage();
      if (jmsMessage instanceof TextMessage) {
         TextMessage message = (TextMessage) jmsMessage;
         perfCakeMessage.setPayload(message.getText());
      } else if (jmsMessage instanceof ObjectMessage) {
         ObjectMessage message = (ObjectMessage) jmsMessage;
         perfCakeMessage.setPayload(message.getObject());
      } else {
         throw new UnsupportedOperationException("Unrecognized JMS Message type: " + jmsMessage.getClass().getCanonicalName());
      }

      @SuppressWarnings("rawtypes")
      Enumeration propNames = jmsMessage.getPropertyNames();
      while (propNames.hasMoreElements()) {
         // All JMS property types can be read as String
         String propName = propNames.nextElement().toString();
         String propVal = jmsMessage.getStringProperty(propName);
         perfCakeMessage.setProperty(propName, propVal);
      }

      perfCakeMessage.setHeader("JMSCorrelationID", jmsMessage.getJMSCorrelationID());
      perfCakeMessage.setHeader("JMSMessageID", jmsMessage.getJMSMessageID());
      return perfCakeMessage;
   }

}
