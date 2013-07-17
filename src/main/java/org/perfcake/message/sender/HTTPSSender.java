/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.message.sender;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.util.Utils;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Filip Eliáš <elfilip01@gmail.com>
 */
public class HTTPSSender extends HTTPSender {

   private String keyStore;
   private String keyStorePassword;
   private String trustStore;
   private String trustStorePassword;
   private SSLSocketFactory sslFactory;

   @Override
   public void init() throws Exception {
      super.init();
      sslFactory = initKeyStores();
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      ((HttpsURLConnection) requestConnection).setSSLSocketFactory(sslFactory);
   }

   private KeyStore initKeyStore(String keyStoreLocation, String keyStorePassword) throws Exception {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      try (InputStream is = Utils.locationToUrl(keyStoreLocation, "perfcake.scenarios.dir", Utils.determineDefaultLocation("keystores"), "").openStream()) {
         keyStore.load(is, keyStorePassword.toCharArray());
      }

      return keyStore;
   }

   private SSLSocketFactory initKeyStores() throws Exception {
      KeyStore keyStore_ = null;
      KeyStore trustStore_ = null;
      KeyManagerFactory keyManager = null;
      TrustManagerFactory trustManager = null;

      if (keyStore != null) {
         if (keyStorePassword == null) {
            throw new PerfCakeException("The keyStore password is not set. (Use keyStorePassword property of the HTTPSSender to set it!)");
         } else {
            keyStore_ = initKeyStore(keyStore, keyStorePassword);
            keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManager.init(keyStore_, keyStorePassword.toCharArray());
         }
      }

      if (trustStore != null) {
         if (trustStorePassword == null) {
            throw new PerfCakeException("The trustStore password is not set. (Use trustStorePassword property of the HTTPSSender to set it!)");
         } else {
            trustStore_ = initKeyStore(trustStore, trustStorePassword);
            trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManager.init(trustStore_);
         }
      }

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(keyManager == null ? null : keyManager.getKeyManagers(), trustManager == null ? null : trustManager.getTrustManagers(), null);

      return ctx.getSocketFactory();
   }

   public String getKeyStore() {
      return keyStore;
   }

   public void setKeyStore(String keyStore) {
      this.keyStore = keyStore;
   }

   public String getKeyStorePassword() {
      return keyStorePassword;
   }

   public void setKeyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
   }

   public String getTrustStore() {
      return trustStore;
   }

   public void setTrustStore(String trustStore) {
      this.trustStore = trustStore;
   }

   public String getTrustStorePassword() {
      return trustStorePassword;
   }

   public void setTrustStorePassword(String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
   }

}
