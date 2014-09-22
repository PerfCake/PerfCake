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

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Properties;
import javax.annotation.Resource;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;

/**
 * @author Lenka Vašková <vaskova.lenka@gmail.com>
 */
public class RequestResponseJmsSenderTest extends Arquillian {

   private static final Logger log = Logger.getLogger(RequestResponseJmsSenderTest.class);

   @Resource(mappedName = "queue/test")
   private Queue queue;

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

   @Test
   public void testResponseSend() throws Exception {
      String queueName = "queue/test";
      String replyQueueName = "queue/test_reply";

      JmsHelper.Wiretap wiretap = JmsHelper.wiretap(queueName, replyQueueName);
      wiretap.start();

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", queueName);
      props.setProperty("responseTarget", replyQueueName);
      props.setProperty("connectionFactory", "ConnectionFactory");
      props.setProperty("transacted", "true");
      props.setProperty("autoAck", "false");

      RequestResponseJmsSender sender = (RequestResponseJmsSender) ObjectFactory.summonInstance(RequestResponseJmsSender.class.getName(), props);

      Assert.assertEquals(sender.getMessageType(), RequestResponseJmsSender.MessageType.STRING);
      Assert.assertEquals(sender.getTarget(), queueName);
      Assert.assertEquals(sender.getResponseTarget(), replyQueueName);
      Assert.assertEquals(sender.isTransacted(), true);
      Assert.assertEquals(sender.isAutoAck(), false);

      try {
         sender.init();

         // make sure the queues are empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queueReply));

         // STRING
         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload = "Hello World!";
         message.setPayload(payload);
         sender.preSend(message, null);
         Serializable response = sender.send(message, null);
         sender.postSend(message);

         Assert.assertTrue(response instanceof String);
         Assert.assertEquals((String) response, payload);

         // what did the wiretap see
         Message jmsResponse = wiretap.getLastMessage();
         Assert.assertTrue(jmsResponse instanceof TextMessage);
         Assert.assertEquals(((TextMessage) jmsResponse).getText(), payload);

         // OBJECT
         sender.setMessageType(JmsSender.MessageType.OBJECT);
         message = new org.perfcake.message.Message();
         Long payloadObject = 42L;
         message.setPayload(payloadObject);
         sender.preSend(message, null);
         response = sender.send(message, null);
         sender.postSend(message);

         Assert.assertTrue(response instanceof Long);
         Assert.assertEquals((Long) response, payloadObject);

         // what did the wiretap see
         jmsResponse = wiretap.getLastMessage();
         Assert.assertTrue(jmsResponse instanceof ObjectMessage);
         Assert.assertTrue(((ObjectMessage) jmsResponse).getObject() instanceof Long);
         Assert.assertEquals((Long) ((ObjectMessage) jmsResponse).getObject(), payloadObject);

         // BYTEARRAY
         sender.setMessageType(JmsSender.MessageType.BYTEARRAY);
         message = new org.perfcake.message.Message();
         message.setPayload(payload);
         sender.preSend(message, null);
         response = sender.send(message, null);
         sender.postSend(message);

         Assert.assertTrue(response instanceof byte[]);
         Assert.assertEquals(new String((byte[]) response, "UTF-8").trim(), payload);

         // what did the wiretap see
         jmsResponse = wiretap.getLastMessage();
         Assert.assertTrue(jmsResponse instanceof BytesMessage);
         BytesMessage bytesMessage = (BytesMessage) jmsResponse;
         bytesMessage.reset();
         byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
         bytesMessage.readBytes(bytes, bytes.length);
         Assert.assertEquals(new String(bytes, "UTF-8").trim(), payload);

         wiretap.stop();

         // make sure the queue is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queueReply));
      } finally {
         sender.close();
      }
   }

   @Test
   public void testCorrelationId() throws Exception {
      String queueName = "queue/test";
      String replyQueueName = "queue/test_reply";

      JmsHelper.Wiretap wiretap = JmsHelper.wiretap(queueName, replyQueueName);
      wiretap.start();

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", queueName);
      props.setProperty("responseTarget", replyQueueName);
      props.setProperty("useCorrelationId", "true");

      RequestResponseJmsSender sender = (RequestResponseJmsSender) ObjectFactory.summonInstance(RequestResponseJmsSender.class.getName(), props);

      Assert.assertEquals(sender.getMessageType(), RequestResponseJmsSender.MessageType.STRING);
      Assert.assertEquals(sender.getTarget(), queueName);
      Assert.assertEquals(sender.getResponseTarget(), replyQueueName);
      Assert.assertEquals(sender.isUseCorrelationId(), true);

      try {
         sender.init();

         // make sure the queue is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queueReply));

         // send colliding message
         String collidePayload = "Collide Hello World!";
         Properties collideProps = new Properties();
         collideProps.setProperty("messagetType", "STRING");
         collideProps.setProperty("target", replyQueueName);
         JmsSender collideSender = (JmsSender) ObjectFactory.summonInstance(JmsSender.class.getName(), collideProps);

         try {
            collideSender.init();

            org.perfcake.message.Message message = new org.perfcake.message.Message();
            message.setPayload(collidePayload);
            collideSender.preSend(message, null);
            collideSender.send(message, null);
            collideSender.postSend(message);
         } finally {
            collideSender.close();
         }

         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload = "Correlating Hello World!";
         message.setPayload(payload);
         sender.preSend(message, null);
         Serializable response = sender.send(message, null);
         sender.postSend(message);

         Assert.assertTrue(response instanceof String);
         Assert.assertEquals((String) response, payload);

         wiretap.stop();

         Message collidingMessage = JmsHelper.readMessage(factory, 500, queueReply);
         Assert.assertTrue(collidingMessage instanceof TextMessage);
         Assert.assertEquals(((TextMessage) collidingMessage).getText(), collidePayload);

         // make sure the queues are empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queueReply));
      } finally {
         sender.close();
      }
   }

   @Test
   public void testNegativeTimeout() throws Exception {
      String queueName = "queue/test";
      String replyQueueName = "queue/test_reply";

      Properties props = new Properties();
      props.setProperty("messagetType", "STRING");
      props.setProperty("target", queueName);
      props.setProperty("responseTarget", replyQueueName);
      props.setProperty("receivingTimeout", "10");
      props.setProperty("receiveAttempts", "2");

      RequestResponseJmsSender sender = (RequestResponseJmsSender) ObjectFactory.summonInstance(RequestResponseJmsSender.class.getName(), props);

      Assert.assertEquals(sender.getMessageType(), RequestResponseJmsSender.MessageType.STRING);
      Assert.assertEquals(sender.getTarget(), queueName);
      Assert.assertEquals(sender.getResponseTarget(), replyQueueName);
      Assert.assertEquals(sender.getReceivingTimeout(), 10);
      Assert.assertEquals(sender.getReceiveAttempts(), 2);

      try {
         sender.init();

         // make sure the queue is empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queueReply));

         org.perfcake.message.Message message = new org.perfcake.message.Message();
         String payload = "Timeout Hello World!";
         message.setPayload(payload);
         sender.preSend(message, null);
         try {
            Serializable response = sender.send(message, null);
            Assert.assertFalse(true, "The expected exception was not thrown.");
         } catch (PerfCakeException pce) {
            Assert.assertTrue(pce.getMessage().contains("No message"));
         }

         // read the original message from the queue
         Message originalMessage = JmsHelper.readMessage(factory, 500, queue);
         Assert.assertTrue(originalMessage instanceof TextMessage);
         Assert.assertEquals(((TextMessage) originalMessage).getText(), payload);

         // make sure the queues are empty
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queue));
         Assert.assertNull(JmsHelper.readMessage(factory, 500, queueReply));
      } finally {
         sender.close();
      }

   }
}
