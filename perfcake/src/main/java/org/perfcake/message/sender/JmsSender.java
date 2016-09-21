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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Properties;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
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
public class JmsSender extends AbstractJmsSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(JmsSender.class);

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
            if (transacted) {
               context = qcf.createContext(username, password, JMSContext.SESSION_TRANSACTED);
            } else {
               context = qcf.createContext(username, password);
            }
         } else {
            if (transacted) {
               context = qcf.createContext(JMSContext.SESSION_TRANSACTED);
            } else {
               context = qcf.createContext();
            }
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

   @SuppressWarnings("Duplicates") // false positive with Jms11Sender
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
      setMessageProperties(message, messageAttributes);
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
}
