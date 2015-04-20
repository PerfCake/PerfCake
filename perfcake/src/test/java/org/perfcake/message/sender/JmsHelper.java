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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Test JMS client, both in-jvm and separate one. Also provides a wiretap that can read messages from a queue and pass them to another immediately.
 *
 * @author <a href="mailto:vaskova.lenka@gmail.com">Lenka Vašková</a>
 */
public class JmsHelper {

   public static class Wiretap implements Runnable {

      private static final Logger log = LogManager.getLogger(Wiretap.class);

      private Thread master;
      private final String inQueue;
      private final String outQueue;
      private Message message = null;
      private boolean running = false;

      private Wiretap(final String inQueue, final String outQueue) {
         this.inQueue = inQueue;
         this.outQueue = outQueue;
      }

      private void setMaster(final Thread master) {
         this.master = master;
      }

      public Message getLastMessage() {
         return message;
      }

      public void start() {
         running = true;
         master.start();
      }

      public void stop() {
         master.interrupt();
         running = false;
      }

      @Override
      public void run() {
         Context context = null;
         try {
            context = new InitialContext();
            final ConnectionFactory cf = (ConnectionFactory) context.lookup("ConnectionFactory");
            final Destination source = (Destination) context.lookup(inQueue);
            final Destination target = (Destination) context.lookup(outQueue);

            Connection connection = null;
            Session session = null;

            try {
               connection = cf.createConnection();
               connection.start();
               session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
               final MessageConsumer messageConsumer = session.createConsumer(source);
               final MessageProducer messageProducer = session.createProducer(target);

               if (master == null) {
                  throw new IllegalStateException("First set the master thread.");
               }

               while (!master.isInterrupted() && running) {
                  final Message message = messageConsumer.receive(1000);
                  if (message != null) {
                     this.message = message;
                     messageProducer.send(message);
                  }
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
         } catch (final Exception e) {
            if (e.getCause() instanceof InterruptedException) {
               log.info("Terminating gracefully.");
            } else {
               log.error("Error during wiretap:", e);
            }
         } finally {
            try {
               if (context != null) {
                  context.close();
               }
            } catch (final NamingException ne) {
               log.error("Error closing initial context:", ne);
            }
         }
      }
   }

   public static Wiretap wiretap(final String inQueue, final String outQueue) {
      final Wiretap w = new Wiretap(inQueue, outQueue);
      final Thread t = new Thread(w);
      w.setMaster(t);

      return w;
   }

   public static Message readMessage(final ConnectionFactory factory, final long timeout, final Queue queue) throws JMSException {
      Connection connection = null;
      Session session = null;
      Message message = null;

      try {
         connection = factory.createConnection();
         connection.start();
         session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         final MessageConsumer messageConsumer = session.createConsumer(queue);
         message = messageConsumer.receive(timeout);
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

      return message;
   }

   public static Message clientReadMessage(final long timeout, final String queueName) throws Exception {
      final Properties env = new Properties();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
      env.put(Context.PROVIDER_URL, "http-remoting://localhost:8080");
      env.put(Context.SECURITY_PRINCIPAL, "zappa");
      env.put(Context.SECURITY_CREDENTIALS, "frank");

      Message message = null;
      final Context context = new InitialContext(env);
      try {
         final ConnectionFactory cf = (ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
         final Destination destination = (Destination) context.lookup("jms/queue/test");

         Connection connection = null;
         Session session = null;

         try {
            connection = cf.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageConsumer messageConsumer = session.createConsumer(destination);
            message = messageConsumer.receive(timeout);
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
      } finally {
         context.close();
      }

      return message;
   }
}
