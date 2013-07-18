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
import java.util.Properties;
import java.util.Set;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;

/**
 * The sender that is able to send messages via JMS.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Jiří Sedláček <jiri@sedlackovi.cz>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public abstract class JMSSender extends AbstractSender {

   /**
    * The logger's logger.
    */
   private static final Logger log = Logger.getLogger(JMSSender.class);

   /**
    * JMS initial context.
    */
   protected InitialContext ctx = null;

   /**
    * JMS queue connection factory.
    */
   protected QueueConnectionFactory qcf = null;

   /**
    * JMS connection.
    */
   protected QueueConnection connection;

   /**
    * JMS session
    */
   protected QueueSession session;

   /**
    * JMS queue where the messages are send.
    */
   protected Queue queue;

   /**
    * JMS queue sender.
    */
   protected QueueSender sender;

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
    * Indicates whether the JMS message is auto-acknowledged by the reciever (true) or by client (false).
    */
   protected boolean autoAck = true;

   /**
    * Specifies that the payload should be send as an {@link javax.jms.ObjectMessage} (true)
    * or {@link javax.jms.TextMessage} (false).
    */
   protected boolean sendAsObject = false;

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
    * Creates a new instance of JMSSender.
    */
   public JMSSender() {
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

         Object tmp = ctx.lookup(connectionFactory);
         qcf = (QueueConnectionFactory) tmp;
         if (checkCredentials()) {
            connection = qcf.createQueueConnection(username, password);
         } else {
            connection = qcf.createQueueConnection();
         }
         queue = (Queue) ctx.lookup(target);
         if (replyTo != null && !"".equals(replyTo)) {
            replyToDestination = (Destination) ctx.lookup(replyTo);
         }
         if (autoAck) {
            session = connection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
         } else {
            session = connection.createQueueSession(transacted, Session.CLIENT_ACKNOWLEDGE);
         }
         connection.start();
         sender = session.createSender(queue);
         sender.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
      } catch (Exception e) {
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
                  if (connection != null) {
                     connection.close();
                  }
               }
            }
         }

      } catch (JMSException e) {
         throw new PerfCakeException(e);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(org.perfcake.message.Message message, Map<String, String> properties) throws Exception {
      if (!sendAsObject) {
         mess = session.createTextMessage((String) message.getPayload());
      } else {
         mess = session.createObjectMessage(message.getPayload());
      }
      Set<String> propertyNameSet = message.getProperties().stringPropertyNames();
      for (String property : propertyNameSet) {
         mess.setStringProperty(property, message.getProperty(property));
      }
      // set additional properties
      if (properties != null) {
         propertyNameSet = properties.keySet();
         for (String property : propertyNameSet) {
            mess.setStringProperty(property, message.getProperty(property));
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
   public Serializable doSend(org.perfcake.message.Message message, Map<String, String> properties) throws Exception {
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
    * Checks if JMS credentials ({@link #username} and {@link #password}) are set.
    * 
    * @return <code>true</code> if both JMS credentials ({@link #username} and {@link #password})
    *         are set and <code>false</code> if neither of them is set.
    * @throws PerfCakeException
    *            If one of the credentials is not set.
    */
   protected boolean checkCredentials() throws PerfCakeException {
      if (username == null && password == null) {
         return false;
      } else if ((username == null && password != null) || (username != null && password == null)) {
         throw new PerfCakeException("For Secured JMS message, both username and password must be set.");
      } else {
         return true;
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message)
    */
   @Override
   public Serializable doSend(org.perfcake.message.Message message) throws Exception {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#postSend(org.perfcake.message.Message)
    */
   @Override
   public void postSend(org.perfcake.message.Message message) throws Exception {

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
    *           The username to set.
    */
   public void setUsername(String username) {
      this.username = username;
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
    *           The password to set.
    */
   public void setPassword(String password) {
      this.password = password;
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
    *           The transacted to set.
    */
   public void setTransacted(boolean transacted) {
      this.transacted = transacted;
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
    *           The persistent to set.
    */
   public void setPersistent(boolean persistent) {
      this.persistent = persistent;
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
   public void setAutoAck(boolean autoAck) {
      this.autoAck = autoAck;
   }

   /**
    * Used to read the value of sendAsObject.
    * 
    * @return The sendAsObject.
    */
   public boolean isSendAsObject() {
      return sendAsObject;
   }

   /**
    * Sets the value of sendAsObject.
    * 
    * @param sendAsObject
    *           The sendAsObject to set.
    */
   public void setSendAsObject(boolean sendAsObject) {
      this.sendAsObject = sendAsObject;
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
    *           The connectionFactory to set.
    */
   public void setConnectionFactory(String connectionFactory) {
      this.connectionFactory = connectionFactory;
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
    *           The jndiContextFactory to set.
    */
   public void setJndiContextFactory(String jndiContextFactory) {
      this.jndiContextFactory = jndiContextFactory;
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
    *           The jndiUrl to set.
    */
   public void setJndiUrl(String jndiUrl) {
      this.jndiUrl = jndiUrl;
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
    *           The jndiSecurityPrincipal to set.
    */
   public void setJndiSecurityPrincipal(String jndiSecurityPrincipal) {
      this.jndiSecurityPrincipal = jndiSecurityPrincipal;
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
    *           The jndiSecurityCredentials to set.
    */
   public void setJndiSecurityCredentials(String jndiSecurityCredentials) {
      this.jndiSecurityCredentials = jndiSecurityCredentials;
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
    *           The replyTo to set.
    */
   public void setReplyTo(String replyTo) {
      this.replyTo = replyTo;
   }

}
