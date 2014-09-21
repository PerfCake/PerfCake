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
import org.perfcake.util.ObjectFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Resource;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;

/**
 * @author Lenka Vašková <vaskova.lenka@gmail.com>
 */
public class JmsSenderTest extends Arquillian {

   @Resource(mappedName = "queue/test")
   private Queue queue;

   @Resource(mappedName = "queue/secured_test")
   private Queue securedQueue;

   @Resource(mappedName = "queue/test_reply")
   private Queue queueReply;

   @Resource(mappedName = "java:/ConnectionFactory")
   private ConnectionFactory factory;

   @Deployment
   public static JavaArchive createDeployment() {
      return ShrinkWrap.create(JavaArchive.class).addPackages(true,
            "org.perfcake",
            "org.apache.commons.beanutils",
            "org.apache.log4j",
            "org.apache.commons.collections");
   }

   @Test(priority = 0)
   public void testSanityCheck() throws Exception {
      Assert.assertNotNull(factory, "Unable to inject connection factory.");
      Assert.assertNotNull(queue, "Unable to inject destination destination/test.");
   }

   @Test
   public void testBasicSend() throws Exception {
      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/test");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      Assert.assertEquals(sender.getMessageType(), JmsSender.MessageType.STRING);
      Assert.assertEquals(sender.getTarget(), "queue/test");
      Assert.assertEquals(sender.isPersistent(), true);
      Assert.assertEquals(sender.isTransacted(), false);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload1 = "Hello World!";
         message.setPayload(payload1);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertEquals(response.getJMSDeliveryMode(), DeliveryMode.PERSISTENT);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload1);

         // OBJECT Type
         sender.setMessageType(JmsSender.MessageType.OBJECT);
         Long payload2 = 42L;
         message.setPayload(payload2);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof ObjectMessage);
         Assert.assertTrue(((ObjectMessage) response).getObject() instanceof Long);
         Assert.assertEquals((Long) ((ObjectMessage) response).getObject(), payload2);

         // BYTEARRAY Type
         sender.setMessageType(JmsSender.MessageType.BYTEARRAY);
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
      String payload = "Hello my secret World!";

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/secured_test");
      props.setProperty("username", "frank");
      props.setProperty("password", "frank");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      Assert.assertEquals(sender.getUsername(), "frank");
      Assert.assertEquals(sender.getPassword(), "frank");

      sender.setTransacted(true);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, securedQueue));

         // STRING Type
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         // make sure the destination is empty because the message is not yet commited (done in close())
         Assert.assertNull(JmsHelper.readMessage(factory, 500, securedQueue));
      } finally {
         sender.close();
      }

      Message response = JmsHelper.readMessage(factory, 500, securedQueue);
      Assert.assertEquals(response.getJMSDeliveryMode(), DeliveryMode.PERSISTENT);
      Assert.assertTrue(response instanceof TextMessage);
      Assert.assertEquals(((TextMessage) response).getText(), payload);

      // make sure the destination is empty
      Assert.assertNull(JmsHelper.readMessage(factory, 500, securedQueue));
   }

   @Test
   public void testNonPersistentDelivery() throws Exception {
      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/test");
      props.setProperty("persistent", "false");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      Assert.assertEquals(sender.isPersistent(), false);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // NON-PERSISTENT delivery
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload1 = "Hello World!";
         message.setPayload(payload1);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
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
      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/test");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // CLIENT-ACK
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload = "Hello World Client Ack!";
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
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
      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/secured_test");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);
      sender.setUsername("frank");
      sender.setPassword(null);

      try {
         sender.init();
         Assert.assertFalse(true, "The expected exception was not thrown.");
      } catch (PerfCakeException perfEx) {
         Assert.assertTrue(perfEx.getMessage().contains("both"));
      }

      sender.setUsername(null);
      sender.setPassword("frank");

      try {
         sender.init();
         Assert.assertFalse(true, "The expected exception was not thrown.");
      } catch (PerfCakeException perfEx) {
         Assert.assertTrue(perfEx.getMessage().contains("both"));
      }

   }

   @Test
   public void testSecuredNegativWrongPassword() throws Exception {
      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/secured_test");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);
      sender.setUsername("frank");
      sender.setPassword("wrongPassword");

      try {
         sender.init();
         Assert.assertFalse(true, "The expected exception was not thrown.");
      } catch (PerfCakeException perfEx) {
         Assert.assertTrue(perfEx.getMessage().contains("validate"));
      }

   }

   @Test
   public void testProperties() throws Exception {

      String payload = "Hello World with Properties!";

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/test");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         message.setProperty("kulíšek", "kulíšek nejmenší");
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);
         Assert.assertEquals(response.getStringProperty("kulíšek"), "kulíšek nejmenší");

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }
   }

   @Test
   @RunAsClient
   public void testClientMode() throws Exception {
      String jndiFactory = org.jboss.naming.remote.client.InitialContextFactory.class.getName();
      String jndiUrl = "remote://localhost:4447";
      String queueName = "jms/queue/test";

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", queueName);
      props.setProperty("jndiContextFactory", jndiFactory);
      props.setProperty("jndiUrl", jndiUrl);
      props.setProperty("jndiSecurityPrincipal", "zappa");
      props.setProperty("jndiSecurityCredentials", "frank");
      props.setProperty("connectionFactory", "jms/RemoteConnectionFactory");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);
      Assert.assertEquals(sender.getJndiContextFactory(), jndiFactory);
      Assert.assertEquals(sender.getJndiUrl(), jndiUrl);
      Assert.assertEquals(sender.getJndiSecurityPrincipal(), "zappa");
      Assert.assertEquals(sender.getJndiSecurityCredentials(), "frank");
      Assert.assertEquals(sender.getConnectionFactory(), "jms/RemoteConnectionFactory");

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.clientReadMessage(500, queueName));

         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload = "Hello from Client!";
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.clientReadMessage(500, queueName);
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
      String payload = "Hello World with  additional Properties!";

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/test");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         Map<String, String> mapProps = new HashMap<>();
         mapProps.put("kulisek", "kulisek nejmensi");
         sender.preSend(message, mapProps);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
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
      String payload = "Hello World from Reply to!";

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", "queue/test");
      props.setProperty("replyTo", "queue/test_reply");

      JmsSender sender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), props);

      try {
         sender.init();

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));

         // STRING Type
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         message.setPayload(payload);
         sender.preSend(message, null);
         sender.send(message, null);
         sender.postSend(message);

         Message response = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(response instanceof TextMessage);
         Assert.assertEquals(((TextMessage) response).getText(), payload);
         Assert.assertEquals(sender.getReplyTo(), "queue/test_reply");

         // make sure the destination is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
      } finally {
         sender.close();
      }
   }

}
