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

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * The sender that is able to send messages via JMS.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Marek Baluch <baluch.git@gmail.com>
 */
public class JmsSender extends AbstractSender {

   /**
    * JMS message type.
    */
   public static enum MessageType {
      OBJECT, STRING, BYTEARRAY
   }

   /**
    * The logger's logger.
    */
   private static final Logger log = Logger.getLogger(JmsSender.class);

   /**
    * JMS initial context.
    */
   protected InitialContext ctx = null;

   /**
    * JMS destination connection factory.
    */
   protected ConnectionFactory qcf = null;

   /**
    * JMS connection.
    */
   protected Connection connection;

   /**
    * JMS session
    */
   protected Session session;

   /**
    * JMS destination where the messages are send.
    */
   protected Destination destination;

   /**
    * JMS destination sender.
    */
   protected MessageProducer sender;

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
   protected String connectionFactory = "ConnectionFactory";

   /**
    * JNDI context factory property.
    */
   protected String jndiContextFactory = null;

   /**
    * JNDI URL property.
    */
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

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Initializing...");
      }
      try {
         Properties ctxProps = new Properties();
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
            connection = qcf.createConnection(username, password);
         } else {
            connection = qcf.createConnection();
         }
         destination = (Destination) ctx.lookup(target);
         if (replyTo != null && !"".equals(replyTo)) {
            replyToDestination = (Destination) ctx.lookup(replyTo);
         }
         session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
         connection.start();
         sender = session.createProducer(destination);
         sender.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
      } catch (JMSException | NamingException | RuntimeException e) {
         throw new PerfCakeException(e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() throws PerfCakeException {
      if (log.isDebugEnabled()) {
         log.debug("Closing...");
      }
      try {
         try {
            if (sender != null) {
               sender.close();
            }
         } finally {
            // conn.stop();
            try {
               if (transacted) {
                  session.commit();
               }
            } finally {
               try {
                  if (session != null) {
                     session.close();
                  }
               } finally {
                  try {
                     if (connection != null) {
                        connection.close();
                     }
                  } finally {
                     if (ctx != null) {
                        ctx.close();
                     }
                  }
               }
            }
         }

      } catch (JMSException | NamingException e) {
         throw new PerfCakeException(e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(final org.perfcake.message.Message message, final Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      switch (messageType) {
         case STRING:
            mess = session.createTextMessage((String) message.getPayload());
            break;
         case BYTEARRAY:
            BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage.writeUTF((String) message.getPayload());
            mess = bytesMessage;
            break;
         case OBJECT:
            mess = session.createObjectMessage(message.getPayload());
            break;
      }
      Set<String> propertyNameSet = message.getProperties().stringPropertyNames();
      for (String property : propertyNameSet) {
         mess.setStringProperty(property, message.getProperty(property));
      }
      // set additional properties
      if (properties != null) {
         for (Map.Entry<String, String> entry : properties.entrySet()) {
            mess.setStringProperty(entry.getKey(), entry.getValue());
         }
      }
      if (replyToDestination != null) {
         mess.setJMSReplyTo(replyToDestination);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public Serializable doSend(final org.perfcake.message.Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Sending a message: " + message.getPayload().toString());
      }
      try {
         sender.send(mess);
      } catch (JMSException e) {
         throw new PerfCakeException("JMS Message cannot be sent", e);
      }

      return null;
   }

   /**
    * Checks if both of the provided credentials are set.
    *
    * @return <code>true</code> if both of the credentials
    * are set and <code>false</code> if neither of them is set.
    * @throws PerfCakeException
    *       If one of the credentials is not set.
    */
   protected static boolean checkCredentials(String username, String password) throws PerfCakeException {
      if (username == null && password == null) {
         return false;
      } else if (username == null || password == null) {
         throw new PerfCakeException("For Secured JMS message, both username and password must be set.");
      } else {
         return true;
      }
   }

   /**
    * Used to read the value of username.
    *
    * @return The username.
    */
   public String getUsername() {
      return username;
   }

   /**
    * Sets the value of username.
    *
    * @param username
    *       The username to set.
    */
   public JmsSender setUsername(final String username) {
      this.username = username;
      return this;
   }

   /**
    * Used to read the value of password.
    *
    * @return The password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the value of password.
    *
    * @param password
    *       The password to set.
    */
   public JmsSender setPassword(final String password) {
      this.password = password;
      return this;
   }

   /**
    * Used to read the value of transacted.
    *
    * @return The transacted.
    */
   public boolean isTransacted() {
      return transacted;
   }

   /**
    * Sets the value of transacted.
    *
    * @param transacted
    *       The transacted to set.
    */
   public JmsSender setTransacted(final boolean transacted) {
      this.transacted = transacted;
      return this;
   }

   /**
    * Used to read the value of persistent.
    *
    * @return The persistent.
    */
   public boolean isPersistent() {
      return persistent;
   }

   /**
    * Sets the value of persistent.
    *
    * @param persistent
    *       The persistent to set.
    */
   public JmsSender setPersistent(final boolean persistent) {
      this.persistent = persistent;
      return this;
   }

   /**
    * Set the value of messageType.
    *
    * @param messageType
    */
   public JmsSender setMessageType(MessageType messageType) {
      this.messageType = messageType;
      return this;
   }

   /**
    * Get the value of messageType.
    *
    * @return
    */
   public MessageType getMessageType() {
      return messageType;
   }

   /**
    * Used to read the value of connectionFactory.
    *
    * @return The connectionFactory.
    */
   public String getConnectionFactory() {
      return connectionFactory;
   }

   /**
    * Sets the value of connectionFactory.
    *
    * @param connectionFactory
    *       The connectionFactory to set.
    */
   public JmsSender setConnectionFactory(final String connectionFactory) {
      this.connectionFactory = connectionFactory;
      return this;
   }

   /**
    * Used to read the value of jndiContextFactory.
    *
    * @return The jndiContextFactory.
    */
   public String getJndiContextFactory() {
      return jndiContextFactory;
   }

   /**
    * Sets the value of jndiContextFactory.
    *
    * @param jndiContextFactory
    *       The jndiContextFactory to set.
    */
   public JmsSender setJndiContextFactory(final String jndiContextFactory) {
      this.jndiContextFactory = jndiContextFactory;
      return this;
   }

   /**
    * Used to read the value of jndiUrl.
    *
    * @return The jndiUrl.
    */
   public String getJndiUrl() {
      return jndiUrl;
   }

   /**
    * Sets the value of jndiUrl.
    *
    * @param jndiUrl
    *       The jndiUrl to set.
    */
   public JmsSender setJndiUrl(final String jndiUrl) {
      this.jndiUrl = jndiUrl;
      return this;
   }

   /**
    * Used to read the value of jndiSecurityPrincipal.
    *
    * @return The jndiSecurityPrincipal.
    */
   public String getJndiSecurityPrincipal() {
      return jndiSecurityPrincipal;
   }

   /**
    * Sets the value of jndiSecurityPrincipal.
    *
    * @param jndiSecurityPrincipal
    *       The jndiSecurityPrincipal to set.
    */
   public JmsSender setJndiSecurityPrincipal(final String jndiSecurityPrincipal) {
      this.jndiSecurityPrincipal = jndiSecurityPrincipal;
      return this;
   }

   /**
    * Used to read the value of jndiSecurityCredentials.
    *
    * @return The jndiSecurityCredentials.
    */
   public String getJndiSecurityCredentials() {
      return jndiSecurityCredentials;
   }

   /**
    * Sets the value of jndiSecurityCredentials.
    *
    * @param jndiSecurityCredentials
    *       The jndiSecurityCredentials to set.
    */
   public JmsSender setJndiSecurityCredentials(final String jndiSecurityCredentials) {
      this.jndiSecurityCredentials = jndiSecurityCredentials;
      return this;
   }

   /**
    * Used to read the value of replyTo.
    *
    * @return The replyTo.
    */
   public String getReplyTo() {
      return replyTo;
   }

   /**
    * Sets the value of replyTo.
    *
    * @param replyTo
    *       The replyTo to set.
    */
   public JmsSender setReplyTo(final String replyTo) {
      this.replyTo = replyTo;
      return this;
   }

}
