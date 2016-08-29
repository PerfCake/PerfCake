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

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.util.ObjectFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;
import javax.annotation.Resource;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Tests {@link org.perfcake.message.sender.Jms11Sender}.
 *
 * @author <a href="mailto:lenka@vecerovi.com">Lenka Večeřa</a>
 */
@Test(groups = { "ueber" })
public class Jms11SenderTest extends Arquillian {

   @Resource(mappedName = "jms/queue/test")
   private Queue queue;

   @Resource(mappedName = "jms/queue/secured_test")
   private Queue securedQueue;

   @Resource(mappedName = "jms/queue/test_reply")
   private Queue queueReply;

   @Resource(mappedName = "java:/ConnectionFactory")
   private ConnectionFactory factory;

   private String disableTemplatesProperty;

   @Deployment
   public static JavaArchive createDeployment() {
      return ShrinkWrap.create(JavaArchive.class).addPackages(true,
            "org.perfcake",
            "javassist",
            "org.apache.commons.beanutils",
            "org.apache.logging.log4j",
            "org.apache.commons.collections")
            .addAsResource("log4j2.xml")
            .deleteClass("org.perfcake.message.sender.WebSocketSender").deleteClass("org.perfcake.message.sender.WebSocketSender$PerfCakeClientEndpoint");
   }

   @BeforeClass
   public void disableTemplates() {
      disableTemplatesProperty = System.getProperty(PerfCakeConst.DISABLE_TEMPLATES_PROPERTY);
      System.setProperty(PerfCakeConst.DISABLE_TEMPLATES_PROPERTY, "true");
   }

   @AfterClass
   public void restoreTemplates() {
      if (disableTemplatesProperty != null) {
         System.setProperty(PerfCakeConst.DISABLE_TEMPLATES_PROPERTY, disableTemplatesProperty);
      }
   }

   @Test(priority = 0)
   public void testSanityCheck() throws Exception {
      Assert.assertNotNull(factory, "Unable to inject connection factory.");
      Assert.assertNotNull(queue, "Unable to inject destination destination/test.");
   }

   @Test
   public void testBasicSend() throws Exception {
      final Properties props = new Properties();
      props.setProperty("messageType", "STRING");
      props.setProperty("target", "jms/queue/test");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      Assert.assertEquals(sender.getMessageType(), Jms11Sender.MessageType.STRING);
      Assert.assertEquals(sender.getTarget(), "jms/queue/test");
      Assert.assertEquals(sender.isPersistent(), true);
      Assert.assertEquals(sender.isTransacted(), false);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         final String payload1 = "Hello World!";
         message.setPayload(payload1);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertEquals(response.getJMSDeliveryMode(), DeliveryMode.PERSISTENT);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload1);

         // OBJECT Type
         sender.setMessageType(Jms11Sender.MessageType.OBJECT);
         final Long payload2 = 42L;
         message.setPayload(payload2);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof ObjectMessage);
         Assert.assertTrue(((ObjectMessage) response).getObject() instanceof Long);
         Assert.assertEquals((Long) ((ObjectMessage) response).getObject(), payload2);

         // BYTEARRAY Type
         sender.setMessageType(Jms11Sender.MessageType.BYTEARRAY);
         message.setPayload(payload1);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof BytesMessage);
         Assert.assertEquals(((BytesMessage) response).readUTF(), payload1);

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

      } finally {
         sender.close();
      }
   }

   @Test
   public void testTransactedSecuredSend() throws Exception {
      final String payload = "Hello my secret World!";

      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/secured_test");
      props.setProperty("username", "frank");
      props.setProperty("password", "frank");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      Assert.assertEquals(sender.getUsername(), "frank");
      Assert.assertEquals(sender.getPassword(), "frank");

      sender.setTransacted(true);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, securedQueue));

         // STRING Type
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         // make sure the destination is empty because the message is not yet commited (done in close())
         Assert.assertNull(JmsHelper.readMessage(factory, 500, securedQueue));
      } finally {
         sender.close();
      }

      final Message response = JmsHelper.readMessage(factory, 500, securedQueue);
      Assert.assertEquals(response.getJMSDeliveryMode(), DeliveryMode.PERSISTENT);
      Assert.assertTrue(response instanceof TextMessage);
      Assert.assertEquals(((TextMessage) response).getText(), payload);

      // make sure the destination is empty
      Assert.assertNull(JmsHelper.readMessage(factory, 500, securedQueue));
   }

   @Test
   public void testNonPersistentDelivery() throws Exception {
      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/test");
      props.setProperty("persistent", "false");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      Assert.assertEquals(sender.isPersistent(), false);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // NON-PERSISTENT delivery
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         final String payload1 = "Hello World!";
         message.setPayload(payload1);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         final Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertEquals(response.getJMSDeliveryMode(), DeliveryMode.NON_PERSISTENT);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload1);

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }
   }

   @Test
   public void testClientAck() throws Exception {
      final Properties props = new Properties();
      props.setProperty("messageType", "STRING");
      props.setProperty("target", "jms/queue/test");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // CLIENT-ACK
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         final String payload = "Hello World Client Ack!";
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         final Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }

   }

   @Test
   public void testSecuredNegativMissingCredentials() throws Exception {
      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/secured_test");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);
      sender.setUsername("frank");
      sender.setPassword(null);

      try {
         sender.init();
         Assert.assertFalse(true, "The expected exception was not thrown.");
      } catch (final PerfCakeException perfEx) {
         Assert.assertTrue(perfEx.getMessage().contains("both"));
      }

      sender.setUsername(null);
      sender.setPassword("frank");

      try {
         sender.init();
         Assert.assertFalse(true, "The expected exception was not thrown.");
      } catch (final PerfCakeException perfEx) {
         Assert.assertTrue(perfEx.getMessage().contains("both"));
      }

   }

   @Test
   public void testSecuredNegativWrongPassword() throws Exception {
      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/secured_test");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);
      sender.setUsername("frank");
      sender.setPassword("wrongPassword");

      try {
         sender.init();
         Assert.assertFalse(true, "The expected exception was not thrown.");
      } catch (final PerfCakeException perfEx) {
         Assert.assertTrue(perfEx.getMessage().contains("validate"));
      }

   }

   @Test
   public void testProperties() throws Exception {

      final String payload = "Hello World with Properties!";

      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/test");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         message.setProperty("kulíšek", "kulíšek nejmenší");
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         final Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);
         Assert.assertEquals(response.getStringProperty("kulíšek"), "kulíšek nejmenší");

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }
   }

   /* https://issues.apache.org/jira/browse/ARTEMIS-385
      This bug gets us a timeout while creating session. In WildFly 10.1.0.Final, there is Artemis 1.1.0. The bug is fixed in 1.3.0.
    */
   @Test(enabled = false)
   @RunAsClient
   public void testClientMode() throws Exception {
      final String jndiFactory = "org.jboss.naming.remote.client.InitialContextFactory";
      final String jndiUrl = "http-remoting://127.0.0.1:8080";
      final String queueName = "jms/queue/test";

      final Properties props = new Properties();
      props.setProperty("messageType", "STRING");
      props.setProperty("target", queueName);
      props.setProperty("jndiContextFactory", jndiFactory);
      props.setProperty("jndiUrl", jndiUrl);
      props.setProperty("jndiSecurityPrincipal", "zappa");
      props.setProperty("jndiSecurityCredentials", "frank");
      props.setProperty("connectionFactory", "jms/RemoteConnectionFactory");
      props.setProperty("username", "zappa");
      props.setProperty("password", "frank");
      props.setProperty("transacted", "true");
      props.setProperty("persistent", "false");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);
      Assert.assertEquals(sender.getJndiContextFactory(), jndiFactory);
      Assert.assertEquals(sender.getJndiUrl(), jndiUrl);
      Assert.assertEquals(sender.getJndiSecurityPrincipal(), "zappa");
      Assert.assertEquals(sender.getJndiSecurityCredentials(), "frank");
      Assert.assertEquals(sender.getConnectionFactory(), "jms/RemoteConnectionFactory");

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.clientReadMessage(500, queueName));

         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         final String payload = "Hello from Client!";
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         final Message response = JmsHelper.clientReadMessage(500, queueName);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.clientReadMessage(500, queueName));
      } finally {
         sender.close();
      }
   }

   @Test
   public void testAdditionalProperties() throws Exception {
      final String payload = "Hello World with  additional Properties!";

      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/test");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         final Properties mapProps = new Properties();
         mapProps.setProperty("kulisek", "kulisek nejmensi");
         sender.preSend(message, mapProps);
         sender.send(message, null);
         sender.postSend(message);

         final Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);
         Assert.assertEquals(response.getStringProperty("kulisek"), "kulisek nejmensi");

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }
   }

   @Test
   public void testReplyTo() throws Exception {
      final String payload = "Hello World from Reply to!";

      final Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "jms/queue/test");
      props.setProperty("replyTo", "jms/queue/test_reply");

      final Jms11Sender sender = (Jms11Sender) ObjectFactory.summonInstance(Jms11Sender.class.getName(), props);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         final org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         final Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);
         Assert.assertEquals(sender.getReplyTo(), "jms/queue/test_reply");

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }
   }

   @Test(enabled = false)
   @RunAsClient
   public void standaloneTest() throws Exception {
      String MESSAGE = "Hello, World!";
      String CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
      String DESTINATION = "jms/queue/secured_test";
      Context namingContext = null;
      JMSContext context = null;

      try {

         // Set up the namingContext for the JNDI lookup
         final Properties env = new Properties();
         env.put(Context.INITIAL_CONTEXT_FACTORY,
               "org.jboss.naming.remote.client.InitialContextFactory");
         env.put(Context.PROVIDER_URL, "http-remoting://127.0.0.1:8080?http-upgrade-enabled=true");
         env.put(Context.SECURITY_PRINCIPAL, "zappa");
         env.put(Context.SECURITY_CREDENTIALS, "frank");

         namingContext = new InitialContext(env);

         ConnectionFactory connectionFactory = (ConnectionFactory) namingContext
               .lookup(CONNECTION_FACTORY);
         System.out.println("Got ConnectionFactory " + CONNECTION_FACTORY);

         Destination destination = (Destination) namingContext
               .lookup(DESTINATION);
         System.out.println("Got JMS Endpoint " + DESTINATION);

         // Create the JMS context
         context = connectionFactory.createContext("frank", "frank");

         context.createProducer().send(destination, MESSAGE);
         System.out.println("Sent message " + MESSAGE);

         // Create the JMS consumer
         JMSConsumer consumer = context.createConsumer(destination);
         // Then receive the same number of messages that were sent

         String text = consumer.receiveBody(String.class, 5000);
         if (text == null) {
            System.out.println("No message Received! Maybe another Consumer listening on the Queue ??");
         }
         System.out.println("Received message with content " + text);
         Assert.assertEquals(text, MESSAGE);

      } catch (Exception e) {
         System.out.println(e.getMessage());
         throw e;
      } finally {
         if (namingContext != null) {
            namingContext.close();
         }

         // closing the context takes care of consumer too
         if (context != null) {
            context.close();
         }
      }
   }
}

