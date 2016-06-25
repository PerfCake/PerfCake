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
import org.perfcake.util.SslSocketFactoryFactory;

import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends messages via HTTPs protocol.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:elfilip01@gmail.com">Filip Eliáš</a>
 */
public class HttpsSender extends HttpSender {

   private String keyStore;
   private String keyStorePassword;
   private String trustStore;
   private String trustStorePassword;
   private SSLSocketFactory sslFactory;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      super.doInit(messageAttributes);
      try {
         sslFactory = SslSocketFactoryFactory.newSslSocketFactory(keyStore, keyStorePassword, trustStore, trustStorePassword);
      } catch (Exception e) {
         throw new PerfCakeException("Cannot load key store.", e);
      }
   }

   @Override
   public void preSend(final Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);
      ((HttpsURLConnection) requestConnection).setSSLSocketFactory(sslFactory);
   }

   /**
    * Gets the SSL key store.
    *
    * @return The SSL key store.
    */
   public String getKeyStore() {
      return keyStore;
   }

   /**
    * Sets the SSL key store.
    *
    * @param keyStore
    *       The SSL key store.
    * @return Instance of this to support fluent API.
    */
   public HttpsSender setKeyStore(final String keyStore) {
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
   public HttpsSender setKeyStorePassword(final String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
   }

   /**
    * Gets the SSL trust store.
    *
    * @return The SSL trust store.
    */
   public String getTrustStore() {
      return trustStore;
   }

   /**
    * Sets the SSL trust store.
    *
    * @param trustStore
    *       The SSL trust store.
    * @return Instance of this to support fluent API.
    */
   public HttpsSender setTrustStore(final String trustStore) {
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
   public HttpsSender setTrustStorePassword(final String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
      return this;
   }
}
