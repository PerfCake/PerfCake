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
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.Utils;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

/**
 * MQTT sender.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class MqttSender extends AbstractSender {

   private BlockingConnection mqttConnection;

   private String topicName = null;

   private org.fusesource.mqtt.client.Message mqttResponse = null;
   private boolean isResponseExpected = false;
   private BlockingConnection mqttResponseConnection = null;

   // Properties
   private String qos = QoS.EXACTLY_ONCE.name();
   private String userName = null;
   private String password = null;

   private String responseTarget = null;
   private String responseQos = qos;
   private String responseUserName = null;
   private String responsePassword = null;

   @Override
   public void doInit(Properties messageAttributes) throws PerfCakeException {
      try {
         final URI targetURI = new URI(getTarget());
         final String protocol = targetURI.getScheme();
         final String host = targetURI.getHost();
         final int port = targetURI.getPort();
         topicName = targetURI.getPath().substring(1);
         try {
            final MQTT mqttClient = new MQTT();
            mqttClient.setHost(protocol + "://" + host + ":" + port);
            mqttClient.setConnectAttemptsMax(0);
            mqttClient.setReconnectAttemptsMax(0);
            if (userName != null) {
               mqttClient.setUserName(userName);
            }

            if (password != null) {
               mqttClient.setPassword(password);
            }
            mqttConnection = mqttClient.blockingConnection();
            mqttConnection.connect();

            if (responseTarget != null) {
               isResponseExpected = true;
               final String responseHost;
               final Integer responsePort;
               final String responseTopicName;
               final URI responseTargetURI = new URI(responseTarget);

               if ((responseHost = responseTargetURI.getHost()) != null) {
                  final String responseProtocol = responseTargetURI.getScheme();
                  responsePort = responseTargetURI.getPort();
                  responseTopicName = responseTargetURI.getPath().substring(1);

                  final MQTT mqttResponseClient = new MQTT();
                  mqttResponseClient.setHost(responseProtocol + "://" + responseHost + ":" + responsePort);
                  mqttResponseClient.setConnectAttemptsMax(0);
                  mqttResponseClient.setReconnectAttemptsMax(0);
                  if (responseUserName != null) {
                     mqttResponseClient.setUserName(responseUserName);
                  }

                  if (responsePassword != null) {
                     mqttResponseClient.setPassword(responsePassword);
                  }
                  mqttResponseConnection = mqttResponseClient.blockingConnection();
                  mqttResponseConnection.connect();
               } else {
                  responseTopicName = responseTarget;
                  mqttResponseConnection = mqttConnection;
               }
               final Topic[] responseTopic = { new Topic(responseTopicName, QoS.valueOf(responseQos)) };
               mqttResponseConnection.subscribe(responseTopic);
            }
         } catch (URISyntaxException e) {
            throw new PerfCakeException("Broker's host or port is invalid", e);
         } catch (Exception e) {
            throw new PerfCakeException("Unable to create MQTT connection.", e);
         }
      } catch (URISyntaxException e) {
         throw new PerfCakeException("Invalid target: \"" + getTarget() + "\".", e);
      }
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);

   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties, MeasurementUnit measurementUnit) throws Exception {
      String response = null;
      mqttConnection.publish(topicName, message.getPayload().toString().getBytes(Utils.getDefaultEncoding()), QoS.valueOf(qos.toUpperCase()), false);
      if (isResponseExpected) {
         mqttResponse = mqttResponseConnection.receive();
         if (mqttResponse != null) {
            response = new String(mqttResponse.getPayload(), Utils.getDefaultEncoding());
         }
      }
      return response;
   }

   @Override
   public void postSend(final Message message) throws Exception {
      if (mqttResponse != null) {
         mqttResponse.ack();
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         if (mqttConnection != null) {
            mqttConnection.disconnect();
         }
         if (mqttResponseConnection != null) {
            mqttResponseConnection.disconnect();
         }
      } catch (Exception e) {
         throw new PerfCakeException("Unable to disconnect.", e);
      }
   }

   public String getResponseQos() {
      return responseQos;
   }

   public void setResponseQos(final String responseQos) {
      this.responseQos = responseQos;
   }

   public String getResponseTarget() {
      return responseTarget;
   }

   public void setResponseTarget(final String responseTarget) {
      this.responseTarget = responseTarget;
   }

   public String getQos() {
      return qos;
   }

   public void setQos(final String qos) {
      this.qos = qos;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(final String userName) {
      this.userName = userName;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(final String password) {
      this.password = password;
   }

   public String getResponseUserName() {
      return responseUserName;
   }

   public void setResponseUserName(final String responseUserName) {
      this.responseUserName = responseUserName;
   }

   public String getResponsePassword() {
      return responsePassword;
   }

   public void setResponsePassword(final String responsePassword) {
      this.responsePassword = responsePassword;
   }
}
