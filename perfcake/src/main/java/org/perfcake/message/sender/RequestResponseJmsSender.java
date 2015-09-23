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

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends messages and receives responses over JMS.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RequestResponseJmsSender extends JmsSender {

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(RequestResponseJmsSender.class);

   /**
    * JMS initial context for response reception.
    */
   protected InitialContext responseCtx = null;

   /**
    * JMS destination connection factory for response reception.
    */
   protected ConnectionFactory rcf = null;

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
    * Maximal number of attempts to read the response.
    */
   private int receiveAttempts = 5;

   /**
    * Correlation ID of this sender instance for it to read only the messages that it has sent.
    */
   private final String correlationId = UUID.randomUUID().toString();

   /**
    * Should the correlation ID be used in the JMS communication? Turning it off (false) allows the sender to read any response from the response destination.
    */
   private boolean useCorrelationId = false;

   /**
    * Indicates whether the JMS message is auto-acknowledged by the receiver (true) or by client (false).
    */
   protected boolean autoAck = true;

   /**
    * JMS connection factory property for the response reception.
    */
   protected String responseConnectionFactory = null;

   /**
    * JNDI context factory property for the response reception.
    */
   protected String responseJndiContextFactory = null;

   /**
    * JNDI URL property for the response reception.
    */
   protected String responseJndiUrl = null;

   /**
    * JNDI username property for the response reception.
    */
   protected String responseJndiSecurityPrincipal = null;

   /**
    * JNDI password for the response reception.
    */
   protected String responseJndiSecurityCredentials = null;

   /**
    * JMS username used for the response reception.
    */
   protected String responseUsername = null;

   /**
    * JMS password used for the response reception.
    */
   protected String responsePassword = null;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      super.doInit(messageAttributes);
      try {
         if (responseTarget == null || responseTarget.equals("")) {
            throw new PerfCakeException("responseTarget property is not defined in the scenario or is empty");
         } else {
            initResponseConnection();

            final Destination responseDestination = (Destination) responseCtx.lookup(responseTarget);

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

      } catch (PerfCakeException pce) {
         throw pce;
      } catch (Exception e) {
         throw new PerfCakeException("Cannot initialize response receiver: ", e);
      }
   }

   private void initResponseConnection() throws Exception {
      try {
         final Properties ctxProps = new Properties();
         Utils.setFirstNotNullProperty(ctxProps, Context.PROVIDER_URL, responseJndiUrl, jndiUrl);
         Utils.setFirstNotNullProperty(ctxProps, Context.INITIAL_CONTEXT_FACTORY, responseJndiContextFactory, jndiContextFactory);
         Utils.setFirstNotNullProperty(ctxProps, Context.SECURITY_PRINCIPAL, responseJndiSecurityPrincipal); // we want to be able not to specify security principal even when it is used for sending the messages
         Utils.setFirstNotNullProperty(ctxProps, Context.SECURITY_CREDENTIALS, responseJndiSecurityCredentials);

         if (ctxProps.isEmpty()) {
            responseCtx = new InitialContext();
         } else {
            responseCtx = new InitialContext(ctxProps);
         }

         rcf = (ConnectionFactory) responseCtx.lookup(Utils.getFirstNotNull(responseConnectionFactory, connectionFactory));
         if (checkCredentials(responseUsername, responsePassword)) {
            responseConnection = rcf.createConnection(responseUsername, responsePassword);
         } else {
            responseConnection = rcf.createConnection();
         }
         responseConnection.start();
      } catch (JMSException | NamingException | RuntimeException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         try {
            super.doClose();
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
      } catch (final JMSException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void preSend(final org.perfcake.message.Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);
      if (useCorrelationId) { // set the correlation ID
         mess.setJMSCorrelationID(correlationId);
      }
   }

   @Override
   public Serializable doSend(final org.perfcake.message.Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      // send the request message
      super.doSend(message, properties, measurementUnit);

      try {
         if (transacted) {
            session.commit();
         }
         // receive response
         Serializable retVal = null;
         int attempts = 0;
         do {
            attempts++;
            final Message response = responseReceiver.receive(receivingTimeout);
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
                  final byte[] bytes = new byte[(int) (((BytesMessage) response).getBodyLength())];
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
      } catch (final JMSException e) {
         throw new PerfCakeException(e);
      }
   }

   /**
    * Sets the configuration of using the correlation ID in response retrieval.
    *
    * @param useCorrelationId
    *       When true, only the messages that are response to the original message can be read from the response destination. Otherwise, any response message can be read.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setUseCorrelationId(final boolean useCorrelationId) {
      this.useCorrelationId = useCorrelationId;
      return this;
   }

   /**
    * Gets the configuration of using the correlation ID.
    *
    * @return Whether the sender receives only the response messages with the appropriate correlation ID, i. e. responses to messages sent by this sender instance.
    */
   public boolean isUseCorrelationId() {
      return useCorrelationId;
   }

   /**
    * Gets the number of milliseconds to wait for the response message.
    *
    * @return Number of milliseconds to wait for the response message.
    */
   public long getReceivingTimeout() {
      return receivingTimeout;
   }

   /**
    * Sets the number of milliseconds to wait for the response message.
    *
    * @param receivingTimeout
    *       Number of milliseconds to wait for the response message.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setReceivingTimeout(final long receivingTimeout) {
      this.receivingTimeout = receivingTimeout;
      return this;
   }

   /**
    * Gets the maximum number of attempts to read the response message.
    *
    * @return The maximum number of attempts to read the response message.
    */
   public int getReceiveAttempts() {
      return receiveAttempts;
   }

   /**
    * Sets the maximum number of attempts to read the response message.
    *
    * @param receiveAttempts
    *       The maximum number of attempts to read the response message.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setReceiveAttempts(final int receiveAttempts) {
      this.receiveAttempts = receiveAttempts;
      return this;
   }

   /**
    * Gets the destination where the response message is being read from.
    *
    * @return The name of the response destination.
    */
   public String getResponseTarget() {
      return responseTarget;
   }

   /**
    * Sets the name of the destination where the response messages should be read from.
    *
    * @param responseTarget
    *       The name of the response destination.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseTarget(final String responseTarget) {
      this.responseTarget = responseTarget;
      return this;
   }

   /**
    * Is JMS message delivery automatically acknowledged?
    *
    * @return The automatic acknowledgement mode.
    */
   public boolean isAutoAck() {
      return autoAck;
   }

   /**
    * Enables/disables the automatic acknowledgement of JMS message delivery.
    *
    * @param autoAck
    *       <code>true</code> to enable the automatic acknowledgement.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setAutoAck(final boolean autoAck) {
      this.autoAck = autoAck;
      return this;
   }

   /**
    * Gets the connection factory used for the response reception.
    *
    * @return The connection factory used for the response reception.
    */
   public String getResponseConnectionFactory() {
      return responseConnectionFactory;
   }

   /**
    * Sets the connection factory used for the response reception.
    * When this is not set, the connection factory used to send the messages is used.
    *
    * @param responseConnectionFactory
    *       The connection factory used for the response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseConnectionFactory(final String responseConnectionFactory) {
      this.responseConnectionFactory = responseConnectionFactory;
      return this;
   }

   /**
    * Gets the JNDI context factory used for the response reception.
    *
    * @return JNDI context factory used for the response reception.
    */
   public String getResponseJndiContextFactory() {
      return responseJndiContextFactory;
   }

   /**
    * Sets the JNDI context factory used for the response reception.
    *
    * @param responseJndiContextFactory
    *       The JNDI context factory used for the response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseJndiContextFactory(final String responseJndiContextFactory) {
      this.responseJndiContextFactory = responseJndiContextFactory;
      return this;
   }

   /**
    * Gets the JNDI URL used for the response reception.
    *
    * @return The JNDI URL used for the response reception.
    */
   public String getResponseJndiUrl() {
      return responseJndiUrl;
   }

   /**
    * Sets the JNDI URL used for the response reception.
    * When this is not set, the JNDI properties set for sending messages are taken.
    *
    * @param responseJndiUrl
    *       The JNDI URL used for the response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseJndiUrl(final String responseJndiUrl) {
      this.responseJndiUrl = responseJndiUrl;
      return this;
   }

   /**
    * Gets the JNDI security principal used for the response reception.
    *
    * @return The security principal used for the response reception.
    */
   public String getResponseJndiSecurityPrincipal() {
      return responseJndiSecurityPrincipal;
   }

   /**
    * Sets the JNDI security principal used for the response reception.
    * When this is not set, an unsecured connection is used.
    *
    * @param responseJndiSecurityPrincipal
    *       The security principal used for the response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseJndiSecurityPrincipal(final String responseJndiSecurityPrincipal) {
      this.responseJndiSecurityPrincipal = responseJndiSecurityPrincipal;
      return this;
   }

   /**
    * Gets the JNDI security credentials used for the response reception.
    *
    * @return The JNDI security credentials used for the response reception.
    */
   public String getResponseJndiSecurityCredentials() {
      return responseJndiSecurityCredentials;
   }

   /**
    * Sets the value of the JNDI security credentials for the response reception.
    * When this is not set and JNDI security principal for the response reception was set, empty password is used.
    * Otherwise, an unsecured connection is used.
    *
    * @param responseJndiSecurityCredentials
    *       The JNDI security credentials to be used for the response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseJndiSecurityCredentials(final String responseJndiSecurityCredentials) {
      this.responseJndiSecurityCredentials = responseJndiSecurityCredentials;
      return this;
   }

   /**
    * Gets the JMS username used for response reception.
    *
    * @return The JMS username used for response reception.
    */
   public String getResponseUsername() {
      return responseUsername;
   }

   /**
    * Sets the JMS username used for response reception.
    *
    * @param responseUsername
    *       The JMS username used for response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponseUsername(final String responseUsername) {
      this.responseUsername = responseUsername;
      return this;
   }

   /**
    * Gets the JMS password used for response reception.
    *
    * @return The JMS password used for response reception.
    */
   public String getResponsePassword() {
      return responsePassword;
   }

   /**
    * Sets the JMS password used for response reception.
    *
    * @param responsePassword
    *       The JMS username used for response reception.
    * @return Instance of this for fluent API.
    */
   public RequestResponseJmsSender setResponsePassword(final String responsePassword) {
      this.responsePassword = responsePassword;
      return this;
   }
}
