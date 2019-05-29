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
import org.perfcake.util.SslSocketFactoryFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends messages via a SSL socket.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SslSocketSender extends AbstractSocketSender {

   /**
    * My precious logger.
    */
   private static final Logger log = LogManager.getLogger(SslSocketSender.class);

   /**
    * SSL key store location.
    */
   private String keyStore;

   /**
    * SSL key store password.
    */
   private String keyStorePassword;

   /**
    * SSL trust store location.
    */
   private String trustStore;

   /**
    * SSL trust store password.
    */
   private String trustStorePassword;

   @Override
   protected void openSocket() throws Exception {
      SSLSocketFactory factory = null;

      try {
         if ((keyStore != null && !"".equals(keyStore)) || (trustStore != null && !"".equals(trustStore))) {
            factory = SslSocketFactoryFactory.newSslSocketFactory(keyStore, keyStorePassword, trustStore, trustStorePassword);
         }
      } catch (PerfCakeException e) {
         log.warn("Unable to initialize SSL socket factory, using the default one: ", e);
      } finally {
         if (factory == null) {
            factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
         }
      }

      socket = factory.createSocket(host, port);
      ((SSLSocket) socket).startHandshake();
   }

   /**
    * Gets the SSL key store location.
    *
    * @return The SSL key store location.
    */
   public String getKeyStore() {
      return keyStore;
   }

   /**
    * Sets the SSL key store location.
    *
    * @param keyStore
    *       The SSL key store location.
    * @return Instance of this to support fluent API.
    */
   public SslSocketSender setKeyStore(final String keyStore) {
      this.keyStore = keyStore;
      return this;
   }

   /**
    * Gets the SSL key store password.
    *
    * @return The SSL key store password.
    */
   public String getKeyStorePassword() {
      return keyStorePassword;
   }

   /**
    * Sets the SSL key store password.
    *
    * @param keyStorePassword
    *       The SSL key store password.
    * @return Instance of this to support fluent API.
    */
   public SslSocketSender setKeyStorePassword(final String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
   }

   /**
    * Gets the SSL trust store location.
    *
    * @return The SSL trust store location.
    */
   public String getTrustStore() {
      return trustStore;
   }

   /**
    * Sets the SSL trust store location.
    *
    * @param trustStore
    *       The SSL trust store location.
    * @return Instance of this to support fluent API.
    */
   public SslSocketSender setTrustStore(final String trustStore) {
      this.trustStore = trustStore;
      return this;
   }

   /**
    * Gets the SSL trust store password.
    *
    * @return The SSL trust store password.
    */
   public String getTrustStorePassword() {
      return trustStorePassword;
   }

   /**
    * Sets the SSL trust store password.
    *
    * @param trustStorePassword
    *       The SSL trust store password.
    * @return Instance of this to support fluent API.
    */
   public SslSocketSender setTrustStorePassword(final String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
      return this;
   }
}
