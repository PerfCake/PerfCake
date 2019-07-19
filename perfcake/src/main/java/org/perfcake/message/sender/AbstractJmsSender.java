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
import org.perfcake.util.properties.MandatoryProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.Set;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.InitialContext;

/**
 * Common ancestor of {@link JmsSender} and {@link Jms11Sender}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class AbstractJmsSender extends AbstractSender {

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
   private static final Logger log = LogManager.getLogger(AbstractJmsSender.class);

   /**
    * JMS initial context.
    */
   protected InitialContext ctx = null;

   /**
    * JMS destination connection factory.
    */
   protected ConnectionFactory qcf = null;

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
    * Use safe message property names because some messaging implementations do not allow anything but valid Java identifiers.
    */
   protected boolean safePropertyNames = true;

   protected void setMessageProperties(final org.perfcake.message.Message message, final Properties messageAttributes) throws Exception {
      final Set<String> propertyNameSet = message.getProperties().stringPropertyNames();
      for (final String property : propertyNameSet) {
         mess.setStringProperty(safePropertyNames ? property.replaceAll("[^a-zA-Z0-9_]", "_") : property, message.getProperty(property));
      }

      // set additional properties
      if (messageAttributes != null) {
         for (String prop : messageAttributes.stringPropertyNames()) {
            mess.setStringProperty(safePropertyNames ? prop.replaceAll("[^a-zA-Z0-9_]", "_") : prop, messageAttributes.getProperty(prop));
         }
      }

      final Properties headers = message.getHeaders();
      headers.forEach((k, v) -> {
         try {
            mess.setStringProperty(safePropertyNames ? k.toString().replaceAll("[^a-zA-Z0-9_]", "_") : k.toString(), v.toString());
         } catch (JMSException e) {
            log.warn("Unknown message header " + k.toString() + "=>" + v.toString() + ": ", e);
         }
      });

      if (replyToDestination != null) {
         mess.setJMSReplyTo(replyToDestination);
      }
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
   public AbstractJmsSender setUsername(final String username) {
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
   public AbstractJmsSender setPassword(final String password) {
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
   public AbstractJmsSender setTransacted(final boolean transacted) {
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
   public AbstractJmsSender setPersistent(final boolean persistent) {
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
   public AbstractJmsSender setMessageType(final JmsSender.MessageType messageType) {
      this.messageType = messageType;
      return this;
   }

   /**
    * Get the JMS message type.
    *
    * @return The JMS message type.
    */
   public JmsSender.MessageType getMessageType() {
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
   public AbstractJmsSender setConnectionFactory(final String connectionFactory) {
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
   public AbstractJmsSender setJndiContextFactory(final String jndiContextFactory) {
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
   public AbstractJmsSender setJndiUrl(final String jndiUrl) {
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
   public AbstractJmsSender setJndiSecurityPrincipal(final String jndiSecurityPrincipal) {
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
   public AbstractJmsSender setJndiSecurityCredentials(final String jndiSecurityCredentials) {
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
   public AbstractJmsSender setReplyTo(final String replyTo) {
      this.replyTo = replyTo;
      return this;
   }

   /**
    * Gets the value of same message property name handling. Some messaging implementations do not allow anything but valid Java identifiers.
    * Defaults to true.
    *
    * @return True if and only if the safe message property name handling is on.
    */
   public boolean isSafePropertyNames() {
      return safePropertyNames;
   }

   /**
    * Sets the value of same message property name handling. Some messaging implementations do not allow anything but valid Java identifiers.
    *
    * @param safePropertyNames
    *       True if and only if the safe message property name handling should be switched on.
    * @return Instance of this for fluent API.
    */
   public AbstractJmsSender setSafePropertyNames(final boolean safePropertyNames) {
      this.safePropertyNames = safePropertyNames;
      return this;
   }

}
