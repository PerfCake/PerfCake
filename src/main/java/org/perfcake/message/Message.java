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
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class Message implements Serializable {

   private static final long serialVersionUID = -6537640529774091119L;
   private Properties headers;
   private Properties properties;
   private Serializable payload = null;

   public Message() {
      this.headers = new Properties();
      this.properties = new Properties();
   }

   public Message(final Serializable payload) {
      this();
      this.payload = payload;
   }

   public Properties getProperties() {
      return properties;
   }

   public void setProperties(final Properties properties) {
      this.properties = properties;
   }

   public String getProperty(final String name) {
      return properties.getProperty(name);
   }

   public String getProperty(final String name, final String defaultValue) {
      return properties.getProperty(name, defaultValue);
   }

   public void setProperty(final String name, final String value) {
      properties.setProperty(name, value);
   }

   public Serializable getPayload() {
      return payload;
   }

   public void setPayload(final Serializable payload) {
      this.payload = payload;
   }

   public void setHeaders(final Properties headers) {
      this.headers = headers;
   }

   public Properties getHeaders() {
      return headers;
   }

   public void setHeader(final String name, final String value) {
      headers.put(name, value);
   }

   public String getHeader(final String name) {
      return headers.getProperty(name);
   }

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
      Message m = (Message) obj;
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
