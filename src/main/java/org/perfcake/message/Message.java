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
package org.perfcake.message;

import java.io.Serializable;
import java.util.Properties;

/**
 * Carries a message to be sent and all possible properties any communication endpoint might use or require.
 * A sender implementation is not obliged to work with all the attributes.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class Message implements Serializable {

   private static final long serialVersionUID = -6537640529774091119L;

   /**
    * Headers of the message.
    */
   private Properties headers;

   /**
    * Properties of the message.
    */
   private Properties properties;

   /**
    * Message payload.
    */
   private Serializable payload = null;

   /**
    * Creates an empty message.
    */
   public Message() {
      this.headers = new Properties();
      this.properties = new Properties();
   }

   /**
    * Creates a message with the given payload.
    *
    * @param payload
    *       Message payload.
    */
   public Message(final Serializable payload) {
      this();
      this.payload = payload;
   }

   /**
    * Gets message properties.
    *
    * @return Message properties.
    */
   public Properties getProperties() {
      return properties;
   }

   /**
    * Sets message properties.
    *
    * @param properties
    *       Message properties.
    */
   public void setProperties(final Properties properties) {
      this.properties = properties;
   }

   /**
    * Gets a message property.
    *
    * @param name
    *       Name of the property.
    * @return The value of the property.
    */
   public String getProperty(final String name) {
      return properties.getProperty(name);
   }

   /**
    * Gets a message property, returning a default value when the property is not set.
    *
    * @param name
    *       Name of the property.
    * @param defaultValue
    *       The value to be returned when the property is not set.
    * @return The value of the property.
    */
   public String getProperty(final String name, final String defaultValue) {
      return properties.getProperty(name, defaultValue);
   }

   /**
    * Sets a message property.
    *
    * @param name
    *       Name of the property.
    * @param value
    *       A new value of the property.
    */
   public void setProperty(final String name, final String value) {
      properties.setProperty(name, value);
   }

   /**
    * Gets the message payload.
    *
    * @return The message payload.
    */
   public Serializable getPayload() {
      return payload;
   }

   /**
    * Sets the message payload.
    *
    * @param payload
    *       The message payload.
    */
   public void setPayload(final Serializable payload) {
      this.payload = payload;
   }

   /**
    * Sets the message headers.
    *
    * @param headers
    *       The message headers.
    */
   public void setHeaders(final Properties headers) {
      this.headers = headers;
   }

   /**
    * Gets the message headers.
    *
    * @return The message headers.
    */
   public Properties getHeaders() {
      return headers;
   }

   /**
    * Sets a message header.
    *
    * @param name
    *       The header name.
    * @param value
    *       A new header value.
    */
   public void setHeader(final String name, final String value) {
      headers.put(name, value);
   }

   /**
    * Gets a message header.
    *
    * @param name
    *       The header name.
    * @return The header value.
    */
   public String getHeader(final String name) {
      return headers.getProperty(name);
   }

   /**
    * Gets a message header, returning a default value when the property is not set.
    *
    * @param name
    *       Name of the header.
    * @param defaultValue
    *       The value to be returned when the header is not set.
    * @return The value of the property.
    */
   public String getHeader(final String name, final String defaultValue) {
      return headers.getProperty(name, defaultValue);
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof Message)) {
         return false;
      }
      final Message m = (Message) obj;
      if (!payload.equals(m.payload)) {
         return false;
      }
      if (!headers.equals(m.headers)) {
         return false;
      }
      if (!properties.equals(m.properties)) {
         return false;
      }
      return true;
   }

   @Override
   public int hashCode() {
      return payload.hashCode() + headers.hashCode() + properties.hashCode();
   }

   @Override
   public String toString() {
      return "[payload=[" + payload + "]; headers=" + headers.toString() + "; properties=" + properties.toString() + "]";
   }
}
