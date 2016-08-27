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
package org.perfcake.message.sender;

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.properties.MandatoryProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends messages via JMS.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:baluchw@gmail.com">Marek Baluch</a>
 */
public class JmsSender extends AbstractSender {

   /**
    * JMS message type.
    */
   public enum MessageType {
      /**
       * Object message.
       *
       * @see javax.jms.ObjectMessage
       */
      OBJECT,

      /**
       * String message.
       *
       * @see javax.jms.TextMessage
       */
      STRING,

      /**
       * Byte array message.
       *
       * @see javax.jms.BytesMessage
       */
      BYTEARRAY
   }

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(JmsSender.class);

   /**
    * JMS initial context.
    */
   protected InitialContext ctx = null;

   /**
    * JMS destination connection factory.
    */
   protected ConnectionFactory qcf = null;

   /**
    * JMS context.
    */
   protected JMSContext context;

   /**
    * JMS producer.
    */
   protected JMSProducer producer;

   /**
    * JMS destination where the messages are send.
    */
   protected Destination destination;

   /**
    * JMS username.
    */
   protected String username = null;

   /**
    * JMS password.
    */
   protected String password = null;

   /**
    * JMS replyTo address.
    */
   protected String replyTo = "";

   /**
    * JMS replyTo destination.
    */
   protected Destination replyToDestination = null;

   /**
    * Indicates whether the JMS transport is transacted or not.
    */
   protected boolean transacted = false;

   /**
    * Indicates whether the JMS message is persisted during transport or not.
    */
   protected boolean persistent = true;

   /**
    * Specifies that the payload should be send as one of {@link JmsSender.MessageType}. Default value
    * is set to MessageType.STRING.
    */
   protected MessageType messageType = MessageType.STRING;

   /**
    * JMS connection factory property.
    */
   @MandatoryProperty
   protected String connectionFactory = "ConnectionFactory";

   /**
    * JNDI context factory property.
    */
   @MandatoryProperty
   protected String jndiContextFactory = null;

   /**
    * JNDI URL property.
    */
   @MandatoryProperty
   protected String jndiUrl = null;

   /**
    * JNDI username property.
    */
   protected String jndiSecurityPrincipal = null;

   /**
    * JNDI password.
    */
   protected String jndiSecurityCredentials = null;

   /**
    * JMS message to send.
    */
   protected Message mess = null;

   /**
    * Creates a new instance of JmsSender.
    */
   public JmsSender() {
      super();
   }

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      try {
         final Properties ctxProps = new Properties();
         if (jndiUrl != null) {
            ctxProps.setProperty(Context.PROVIDER_URL, jndiUrl);
         }
         if (jndiContextFactory != null) {
            ctxProps.setProperty(Context.INITIAL_CONTEXT_FACTORY, jndiContextFactory);
         }
         if (jndiSecurityPrincipal != null) {
            ctxProps.setProperty(Context.SECURITY_PRINCIPAL, jndiSecurityPrincipal);
         }
         if (jndiSecurityCredentials != null) {
            ctxProps.setProperty(Context.SECURITY_CREDENTIALS, jndiSecurityCredentials);
         }

         if (ctxProps.isEmpty()) {
            ctx = new InitialContext();
         } else {
            ctx = new InitialContext(ctxProps);
         }

         qcf = (ConnectionFactory) ctx.lookup(connectionFactory);
         if (checkCredentials(username, password)) {
            context = qcf.createContext(username, password, transacted ? JMSContext.SESSION_TRANSACTED : JMSContext.CLIENT_ACKNOWLEDGE);
         } else {
            context = qcf.createContext(transacted ? JMSContext.SESSION_TRANSACTED : JMSContext.CLIENT_ACKNOWLEDGE);
         }
         destination = (Destination) ctx.lookup(safeGetTarget(messageAttributes));
         if (replyTo != null && !"".equals(replyTo)) {
            replyToDestination = (Destination) ctx.lookup(replyTo);
         }
         producer = context.createProducer();
         producer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
      } catch (NamingException | RuntimeException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         try {
            if (context != null) {
               if (transacted) {
                  context.commit();
               }
               context.close();
            }
         } finally {
            if (ctx != null) {
               ctx.close();
            }
         }
      } catch (NamingException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void preSend(final org.perfcake.message.Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);
      switch (messageType) {
         case STRING:
            mess = context.createTextMessage((String) message.getPayload());
            break;
         case BYTEARRAY:
            final BytesMessage bytesMessage = context.createBytesMessage();
            bytesMessage.writeUTF((String) message.getPayload());
            mess = bytesMessage;
            break;
         case OBJECT:
            mess = context.createObjectMessage(message.getPayload());
            break;
      }
      final Set<String> propertyNameSet = message.getProperties().stringPropertyNames();
      for (final String property : propertyNameSet) {
         mess.setStringProperty(property, message.getProperty(property));
      }

      // set additional properties
      if (messageAttributes != null) {
         for (String prop : messageAttributes.stringPropertyNames()) {
            mess.setStringProperty(prop, messageAttributes.getProperty(prop));
         }
      }

      if (replyToDestination != null) {
         mess.setJMSReplyTo(replyToDestination);
      }
   }

   @Override
   public Serializable doSend(final org.perfcake.message.Message message, final MeasurementUnit measurementUnit) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Sending a message: " + message.getPayload().toString());
      }
      try {
         producer.send(destination, mess);
      } catch (final Throwable e) {
         throw new PerfCakeException("JMS Message cannot be sent", e);
      }

      return null;
   }

   /**
    * Checks if both of the provided credentials are set.
    *
    * @param username
    *       The user name credential.
    * @param password
    *       The password credential.
    * @return <code>true</code> if both of the credentials are set and <code>false</code> if neither of them is set.
    * @throws PerfCakeException
    *       If one of the credentials is not set.
    */
   protected static boolean checkCredentials(final String username, final String password) throws PerfCakeException {
      if (username == null && password == null) {
         return false;
      } else if (username == null || password == null) {
         throw new PerfCakeException("For Secured JMS message, both username and password must be set.");
      } else {
         return true;
      }
   }

   /**
    * Gets the JMS username.
    *
    * @return The JMS username.
    */
   public String getUsername() {
      return username;
   }

   /**
    * Sets the JMS username.
    *
    * @param username
    *       The JMS username.
    * @return Instance of this for fluent API.
    */
   public JmsSender setUsername(final String username) {
      this.username = username;
      return this;
   }

   /**
    * Gets the JMS password.
    *
    * @return The JMS password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the JMS password.
    *
    * @param password
    *       The JMS password.
    * @return Instance of this for fluent API.
    */
   public JmsSender setPassword(final String password) {
      this.password = password;
      return this;
   }

   /**
    * Is JMS message delivery transacted?
    *
    * @return The transacted mode.
    */
   public boolean isTransacted() {
      return transacted;
   }

   /**
    * Sets the JMS delivery transaction mode.
    *
    * @param transacted
    *       The transacted mode.
    * @return Instance of this for fluent API.
    */
   public JmsSender setTransacted(final boolean transacted) {
      this.transacted = transacted;
      return this;
   }

   /**
    * Is JMS message persisted?
    *
    * @return <code>true</code> if JMS message is persisted.
    */
   public boolean isPersistent() {
      return persistent;
   }

   /**
    * Enables/disables persistent delivery mode.
    *
    * @param persistent
    *       <code>true</code> to persist JMS messages.
    * @return Instance of this for fluent API.
    */
   public JmsSender setPersistent(final boolean persistent) {
      this.persistent = persistent;
      return this;
   }

   /**
    * Set the JMS message type.
    *
    * @param messageType
    *       The JMS message type.
    * @return Instance of this for fluent API.
    */
   public JmsSender setMessageType(final MessageType messageType) {
      this.messageType = messageType;
      return this;
   }

   /**
    * Get the JMS message type.
    *
    * @return The JMS message type.
    */
   public MessageType getMessageType() {
      return messageType;
   }

   /**
    * Gets the connection factory.
    *
    * @return The connection factory.
    */
   public String getConnectionFactory() {
      return connectionFactory;
   }

   /**
    * Sets the connection factory.
    *
    * @param connectionFactory
    *       The connectionf actory.
    * @return Instance of this for fluent API.
    */
   public JmsSender setConnectionFactory(final String connectionFactory) {
      this.connectionFactory = connectionFactory;
      return this;
   }

   /**
    * Gets the JNDI context factory.
    *
    * @return The JNDI context factory.
    */
   public String getJndiContextFactory() {
      return jndiContextFactory;
   }

   /**
    * Sets the JNDI context factory.
    *
    * @param jndiContextFactory
    *       The JNDI context factory.
    * @return Instance of this for fluent API.
    */
   public JmsSender setJndiContextFactory(final String jndiContextFactory) {
      this.jndiContextFactory = jndiContextFactory;
      return this;
   }

   /**
    * Gets the JNDI URL.
    *
    * @return The JNDI URL.
    */
   public String getJndiUrl() {
      return jndiUrl;
   }

   /**
    * Sets the value of JNDI URL.
    *
    * @param jndiUrl
    *       The JNDI URL.
    * @return Instance of this for fluent API.
    */
   public JmsSender setJndiUrl(final String jndiUrl) {
      this.jndiUrl = jndiUrl;
      return this;
   }

   /**
    * Gets the JNDI username.
    *
    * @return The JNDI username.
    */
   public String getJndiSecurityPrincipal() {
      return jndiSecurityPrincipal;
   }

   /**
    * Sets the JNDI username.
    *
    * @param jndiSecurityPrincipal
    *       The JNDI username.
    * @return Instance of this for fluent API.
    */
   public JmsSender setJndiSecurityPrincipal(final String jndiSecurityPrincipal) {
      this.jndiSecurityPrincipal = jndiSecurityPrincipal;
      return this;
   }

   /**
    * Gets the JNDI password.
    *
    * @return The JNDI password.
    */
   public String getJndiSecurityCredentials() {
      return jndiSecurityCredentials;
   }

   /**
    * Sets the JNDI password.
    *
    * @param jndiSecurityCredentials
    *       The JNDI password.
    * @return Instance of this for fluent API.
    */
   public JmsSender setJndiSecurityCredentials(final String jndiSecurityCredentials) {
      this.jndiSecurityCredentials = jndiSecurityCredentials;
      return this;
   }

   /**
    * Gets the value of <code>replyTo</code> header of the JMS message.
    *
    * @return The <code>replyTo</code> header of the JMS message.
    */
   public String getReplyTo() {
      return replyTo;
   }

   /**
    * Sets the value of <code>replyTo</code> header of the JMS message.
    *
    * @param replyTo
    *       The <code>replyTo</code> header of the JMS message.
    * @return Instance of this for fluent API.
    */
   public JmsSender setReplyTo(final String replyTo) {
      this.replyTo = replyTo;
      return this;
   }
}
