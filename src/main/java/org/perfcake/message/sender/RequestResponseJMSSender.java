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

package org.perfcake.message.sender;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Jiří Sedláček <jiri@sedlackovi.cz>
 */
public class RequestResponseJMSSender extends JMSSender {
   private static final Logger log = Logger.getLogger(RequestResponseJMSSender.class);

   private QueueConnection responseConnection;

   private QueueSession responseSession;

   private Queue responseQueue;

   private QueueReceiver responseReciever;

   private String responseAddress = "";

   private long recievingTimeout = 1000; // default 1s

   private int receiveAttempts = 5;

   @Override
   public void setProperty(String prop, String value) {
      if ("receivingTimeout".equals(prop)) {
         recievingTimeout = Long.valueOf(value);
      } else if ("responseAddress".equals(prop)) {
         responseAddress = value;
      } else if ("receiveAttempts".equals(prop)) {
         receiveAttempts = Integer.valueOf(value);
      } else {
         super.setProperty(prop, value);
      }
   }

   @Override
   public void init() throws Exception {
      super.init();
      try {
         if (responseAddress == null || responseAddress.equals("")) {
            // if (log.isEnabledFor(Level.ERROR)) {
            // log.error("responseAddress property is not defined in the scenario or is empty");
            // }
            throw new PerfCakeException("responseAddress property is not defined in the scenario or is empty");
         } else {
            responseQueue = (Queue) ctx.lookup(responseAddress);
            if (checkCredentials()) {
               responseConnection = qcf.createQueueConnection(username, password);
            } else {
               responseConnection = qcf.createQueueConnection();
            }
            responseConnection.start();
            responseSession = responseConnection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
            responseReciever = responseSession.createReceiver(responseQueue);
         }

      } catch (Exception e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void close() throws PerfCakeException {
      super.close();
      try {
         if (responseReciever != null) {
            responseReciever.close();
         }
         if (transacted) {
            responseSession.commit();
         }
         if (responseSession != null) {
            responseSession.close();
         }
         if (responseConnection != null) {
            responseConnection.close();
         }
      } catch (JMSException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public Serializable doSend(org.perfcake.message.Message message, Map<String, String> properties) throws Exception {
      super.doSend(message, properties);
      try {
         if (transacted) {
            session.commit();
         }
         // receive response
         Serializable retVal = null;
         int attempts = 0;
         do {
            attempts++;
            Message response = responseReciever.receive(recievingTimeout);
            if (response == null) {
               if (log.isDebugEnabled()) {
                  log.debug("No message in " + responseAddress + " received within the specified timeout (" + recievingTimeout + " ms). Retrying (" + attempts + "/" + receiveAttempts + ") ...");
               }
               // throw new TimeoutException("No message in " +
               // responseAddress
               // + " received within the specified timeout (" +
               // recievingTimeout + " ms).");

            } else {

               if (response instanceof ObjectMessage) {
                  retVal = ((ObjectMessage) response).getObject();
               } else if (response instanceof TextMessage) {
                  retVal = ((TextMessage) response).getText();
               } else if (response instanceof BytesMessage) {
                  byte[] bytes = new byte[(int) (((BytesMessage) response).getBodyLength())];
                  ((BytesMessage) response).readBytes(bytes);
                  retVal = bytes;
               } else {
                  // if (log.isEnabledFor(Level.ERROR)) {
                  // log.error("Received message is neither ObjectMessage nor TextMessage but: "
                  // + message.getClass().getName());
                  // }
                  throw new PerfCakeException("Received message is not one of (ObjectMessage, TextMessage, BytesMessage) but: " + response.getClass().getName());
               }
               if (transacted) {
                  responseSession.commit();
               }
            }

         } while (retVal == null && attempts < receiveAttempts);

         if (retVal == null) {
            throw new TimeoutException("No message in " + responseAddress + " received within the specified timeout (" + recievingTimeout + " ms) in " + receiveAttempts + " attempt(s).");
         }

         return retVal;
      } catch (JMSException e) {
         throw new PerfCakeException(e);
      }
   }

}
