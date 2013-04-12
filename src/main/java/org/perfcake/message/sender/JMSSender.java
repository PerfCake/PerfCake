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
 * 
 * @author Martin Večeřa <marvec@gmail.com>
 * @author Jiří Sedláček <jiri@sedlackovi.cz>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public abstract class JMSSender extends AbstractSender {

   private static final Logger log = Logger.getLogger(JMSSender.class);

   protected QueueConnection connection;

   protected QueueSession session;

   protected Queue queue;

   protected QueueSender sender;

   protected String username = null;

   protected String password = null;

   protected Destination replyTo = null;

   protected boolean transacted = false;

   protected boolean persistent = true;

   protected boolean autoAck = true;

   protected String replyToAddress = "";

   protected boolean sendAsObject = false;

   protected InitialContext ctx = null;

   protected QueueConnectionFactory qcf = null;

   protected String connectionFactory = "ConnectionFactory";

   protected String jndiContextFactory = null;

   protected String jndiUrl = null;

   protected String jndiSecurityPrincipal = null;

   protected String jndiSecurityCredentials = null;

   protected Message mess = null;

   public JMSSender() {
      super();
   }

   @Override
   public void setProperty(String prop, String value) {
      if ("persistent".equals(prop)) {
         persistent = Boolean.valueOf(value);
      } else if ("autoAck".equals(prop)) {
         autoAck = Boolean.valueOf(value);
      } else if ("replyTo".equals(prop)) {
         replyToAddress = value;
      } else if ("transacted".equals(prop)) {
         transacted = Boolean.valueOf(value);
      } else if ("sendAsObject".equals(prop)) {
         sendAsObject = Boolean.valueOf(value);
      } else if ("username".equals(prop)) {
         username = value;
      } else if ("password".equals(prop)) {
         password = value;
      } else if ("connectionFactory".equals(prop)) {
         connectionFactory = value;
      } else if ("jndiContextFactory".equals(prop)) {
         jndiContextFactory = value;
      } else if ("jndiUrl".equals(prop)) {
         jndiUrl = value;
      } else if ("jndiSecurityPrincipal".equals(prop)) {
         jndiSecurityPrincipal = value;
      } else if ("jndiSecurityCredentials".equals(prop)) {
         jndiSecurityCredentials = value;
      } else {
         super.setProperty(prop, value);
      }
   }

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
         queue = (Queue) ctx.lookup(address);
         if (replyToAddress != null && !"".equals(replyToAddress)) {
            replyTo = (Destination) ctx.lookup(replyToAddress);
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

   @Override
   public void close() {
      if (log.isDebugEnabled()) {
         log.debug("Closing...");
      }
      try {
         if (sender != null) {
            sender.close();
         }
         // conn.stop();
         if (transacted) {
            session.commit();
         }
         if (session != null) {
            session.close();
         }
         if (connection != null) {
            connection.close();
         }

      } catch (JMSException e) {
         throw new PerfCakeException(e);
      }
   }

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
      if (replyTo != null) {
         mess.setJMSReplyTo(replyTo);
      }
   }

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

   protected boolean checkCredentials() throws PerfCakeException {
      if (username == null && password == null) {
         return false;
      } else if ((username == null && password != null) || (username != null && password == null)) {
         throw new PerfCakeException("For Secured JMS message, both username and password must be set.");
      } else {
         return true;
      }
   }

   @Override
   public Serializable doSend(org.perfcake.message.Message message) throws Exception {
      return null;
   }

   @Override
   public void postSend(org.perfcake.message.Message message) throws Exception {

   }

}
