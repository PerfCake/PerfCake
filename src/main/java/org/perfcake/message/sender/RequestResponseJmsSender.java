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
package org.perfcake.message.sender;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.reporting.MeasurementUnit;

import javax.jms.*;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * A sender that is the same with @{link JmsSender} and adds a response retrieval.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class RequestResponseJmsSender extends JmsSender {

   /**
    * Logger.
    */
   private static final Logger log = Logger.getLogger(RequestResponseJmsSender.class);

   /**
    * JMS connection to the response destination.
    */
   private Connection responseConnection;

   /**
    * JMS session to the response destination.
    */
   private Session responseSession;

   /**
    * JMS consumer for the response destination.
    */
   private MessageConsumer responseReceiver;

   /**
    * Where to read the responses from.
    */
   private String responseTarget = "";

   /**
    * Timeout for receiving the response in ms for a single attempt.
    */
   private long receivingTimeout = 1000; // default 1s

   /**
    * Maximal number of attemts to read the response.
    */
   private int receiveAttempts = 5;

   /**
    * Correlation ID of this sender instance for it to read only the messages that it has sent.
    */
   private String correlationId = UUID.randomUUID().toString();

   /**
    * Should the correlation ID be used in the JMS communication? Turning it off (false) allows the sender to read any response from the response destination.
    */
   private boolean useCorrelationId = false;

   /**
    * Indicates whether the JMS message is auto-acknowledged by the receiver (true) or by client (false).
    */
   protected boolean autoAck = true;

   @Override
   public void init() throws Exception {
      super.init();
      try {
         if (responseTarget == null || responseTarget.equals("")) {
            throw new PerfCakeException("responseTarget property is not defined in the scenario or is empty");
         } else {
            Destination responseDestination = (Destination) ctx.lookup(responseTarget);
            if (checkCredentials()) {
               responseConnection = qcf.createConnection(username, password);
            } else {
               responseConnection = qcf.createConnection();
            }
            responseConnection.start();

            if (transacted && !autoAck) {
               log.warn("AutoAck setting is ignored with a transacted session. Creating a transacted session.");
            }
            responseSession = responseConnection.createSession(transacted, autoAck ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE);

            if (useCorrelationId) {
               responseReceiver = responseSession.createConsumer(responseDestination, "JMSCorrelationID='" + correlationId + "'");
            } else {
               responseReceiver = responseSession.createConsumer(responseDestination);
            }
         }

      } catch (JMSException | NamingException | RuntimeException | PerfCakeException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void close() throws PerfCakeException {
      try {
         try {
            super.close();
         } finally {
            try {
               if (responseReceiver != null) {
                  responseReceiver.close();
               }
            } finally {
               try {
                  if (transacted) {
                     responseSession.commit();
                  }
               } finally {
                  try {
                     if (responseSession != null) {
                        responseSession.close();
                     }
                  } finally {
                     if (responseConnection != null) {
                        responseConnection.close();
                     }
                  }
               }
            }
         }
      } catch (JMSException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void preSend(final org.perfcake.message.Message message, final Map<String, String> properties) throws Exception {
	   super.preSend(message, properties);
	   if (useCorrelationId) {
	      // set the correlation ID
		  mess.setJMSCorrelationID(correlationId);
	   }
		  
   }
   
   @Override
   public Serializable doSend(final org.perfcake.message.Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      // send the request message
      super.doSend(message, properties, mu);

      try {
         if (transacted) {
            session.commit();
         }
         // receive response
         Serializable retVal = null;
         int attempts = 0;
         do {
            attempts++;
            Message response = responseReceiver.receive(receivingTimeout);
            if (response == null) {
               if (log.isDebugEnabled()) {
                  log.debug("No message in " + responseTarget + " received within the specified timeout (" + receivingTimeout + " ms). Retrying (" + attempts + "/" + receiveAttempts + ") ...");
               }
            } else {
               if (!autoAck) {
                  response.acknowledge();
               }

               if (response instanceof ObjectMessage) {
                  retVal = ((ObjectMessage) response).getObject();
               } else if (response instanceof TextMessage) {
                  retVal = ((TextMessage) response).getText();
               } else if (response instanceof BytesMessage) {
                  byte[] bytes = new byte[(int) (((BytesMessage) response).getBodyLength())];
                  ((BytesMessage) response).readBytes(bytes);
                  retVal = bytes;
               } else {
                  throw new PerfCakeException("Received message is not one of (ObjectMessage, TextMessage, BytesMessage) but: " + response.getClass().getName());
               }

               if (transacted) {
                  responseSession.commit();
               }
            }

         } while (retVal == null && attempts < receiveAttempts);

         if (retVal == null) {
            throw new PerfCakeException("No message in " + responseTarget + " received within the specified timeout (" + receivingTimeout + " ms) in " + receiveAttempts + " attempt(s).");
         }

         return retVal;
      } catch (JMSException e) {
         throw new PerfCakeException(e);
      }
   }

   /**
    * Sets the configuration of using the correlation ID in response retrieval.
    * @param useCorrelationId When true, only the messages that are response to the original message can be read from the response destination. Otherwise, any response message can be read.
    */
   public void setUseCorrelationId(boolean useCorrelationId) {
	   this.useCorrelationId = useCorrelationId;
   }

   /**
    * Gets the configuration of using the correlation ID.
    * @return Whether the sender receives only the response messages with the appropriate correlation ID, i. e. responses to messages sent by this sender instance.
    */
   public boolean isUseCorrelationId() {
	   return useCorrelationId;
   }

   /**
    * Gets the number of milliseconds to wait for the response message.
    * @return Number of milliseconds to wait for the response message.
    */
   public long getReceivingTimeout() {
      return receivingTimeout;
   }

   /**
    * Sets the number of milliseconds to wait for the response message.
    * @param receivingTimeout  Number of milliseconds to wait for the response message.
    */
   public void setReceivingTimeout(final long receivingTimeout) {
      this.receivingTimeout = receivingTimeout;
   }

   /**
    * Gets the maximum number of attempts to read the response message.
    * @return The maximum number of attempts to read the response message.
    */
   public int getReceiveAttempts() {
      return receiveAttempts;
   }

   /**
    * Sets the maximum number of attempts to read the response message.
    * @param receiveAttempts The maximum number of attempts to read the response message.
    */
   public void setReceiveAttempts(final int receiveAttempts) {
      this.receiveAttempts = receiveAttempts;
   }

   public String getResponseTarget() {
      return responseTarget;
   }

   public void setResponseTarget(final String responseTarget) {
      this.responseTarget = responseTarget;
   }

   /**
    * Used to read the value of autoAck.
    *
    * @return The autoAck.
    */
   public boolean isAutoAck() {
      return autoAck;
   }

   /**
    * Sets the value of autoAck.
    *
    * @param autoAck
    *           The autoAck to set.
    */
   public void setAutoAck(final boolean autoAck) {
      this.autoAck = autoAck;
   }
}
