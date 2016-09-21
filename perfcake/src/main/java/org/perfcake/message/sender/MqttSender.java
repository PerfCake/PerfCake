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
import org.perfcake.util.StringTemplate;
import org.perfcake.util.Utils;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Sends messages to an MQTT endpoint using Fusesource MQTT client.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MqttSender extends AbstractSender {

   /**
    * MQTT connection.
    */
   private BlockingConnection mqttConnection;

   /**
    * Name of topic where to send messages.
    */
   private String topicName = null;

   /**
    * MQTT response.
    */
   private org.fusesource.mqtt.client.Message mqttResponse = null;

   /**
    * True if and only if we should wait for response.
    */
   private boolean isResponseExpected = false;

   /**
    * MQTT connection for sending responses.
    */
   private BlockingConnection mqttResponseConnection = null;

   /**
    * Required quality of service.
    */
   private String qos = QoS.EXACTLY_ONCE.name();

   /**
    * MQTT server user name.
    */
   private String userName = null;

   /**
    * MQTT server password.
    */
   private String password = null;

   /**
    * Where to read the response from.
    */
   private StringTemplate responseTarget = null;

   /**
    * Response quality of service.
    */
   private String responseQos = qos;

   /**
    * Response server user name.
    */
   private String responseUserName = null;

   /**
    * Response server password.
    */
   private String responsePassword = null;

   @Override
   public void doInit(Properties messageAttributes) throws PerfCakeException {
      try {
         final URI targetUri = new URI(safeGetTarget(messageAttributes));
         final String protocol = targetUri.getScheme();
         final String host = targetUri.getHost();
         final int port = targetUri.getPort();
         topicName = targetUri.getPath().substring(1);
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
               final String safeResponseTarget = messageAttributes == null ? responseTarget.toString() : responseTarget.toString(messageAttributes);
               final URI responseTargetUri = new URI(safeResponseTarget);

               if ((responseHost = responseTargetUri.getHost()) != null) {
                  final String responseProtocol = responseTargetUri.getScheme();
                  responsePort = responseTargetUri.getPort();
                  responseTopicName = responseTargetUri.getPath().substring(1);

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
                  responseTopicName = safeResponseTarget;
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
   public void preSend(final Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);
      mqttResponse = null;
   }

   @Override
   public Serializable doSend(Message message, MeasurementUnit measurementUnit) throws Exception {
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
      super.postSend(message);
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

   /**
    * Gets the required response quality of service.
    * @return The required response quality of service.
    */
   public String getResponseQos() {
      return responseQos;
   }

   /**
    * Sets the required response quality of service.
    * @param responseQos The required response quality of service.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setResponseQos(final String responseQos) {
      this.responseQos = responseQos;
      return this;
   }

   /**
    * Gets where to read the response from.
    * @return Where to read the response from.
    */
   public String getResponseTarget() {
      return responseTarget.toString();
   }

   /**
    * Sets where to read the response from.
    * @param responseTarget Where to read the response from.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setResponseTarget(final String responseTarget) {
      this.responseTarget = new StringTemplate(responseTarget);
      return this;
   }

   /**
    * Gets the required quality of service.
    * @return The required quality of service.
    */
   public String getQos() {
      return qos;
   }

   /**
    * Sets the required quality of service.
    * @param qos The required quality of service.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setQos(final String qos) {
      this.qos = qos;
      return this;
   }

   /**
    * Gets the MQTT server user name.
    * @return the MQTT server user name.
    */
   public String getUserName() {
      return userName;
   }

   /**
    * Sets the MQTT server user name.
    * @param userName The MQTT server user name.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setUserName(final String userName) {
      this.userName = userName;
      return this;
   }

   /**
    * Gets the MQTT server password.
    * @return The MQTT server password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the MQTT server password.
    * @param password The MQTT server password.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setPassword(final String password) {
      this.password = password;
      return this;
   }

   /**
    * Gets the response server user name.
    * @return The response server user name.
    */
   public String getResponseUserName() {
      return responseUserName;
   }

   /**
    * Sets the tesponse server user name.
    * @param responseUserName The response server user name.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setResponseUserName(final String responseUserName) {
      this.responseUserName = responseUserName;
      return this;
   }

   /**
    * Gets the response server password.
    * @return The response server password.
    */
   public String getResponsePassword() {
      return responsePassword;
   }

   /**
    * Sets the response server password.
    * @param responsePassword The response server password.
    * @return Instance of this to support fluent API.
    */
   public MqttSender setResponsePassword(final String responsePassword) {
      this.responsePassword = responsePassword;
      return this;
   }
}
