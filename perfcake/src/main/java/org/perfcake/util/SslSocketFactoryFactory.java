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
package org.perfcake.util;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Factory to create pre-configured SSL socket factories.
 * Searches for the file in the directory specified in the property called like the value of {@link PerfCakeConst#KEYSTORES_DIR_PROPERTY}, then in
 * <code>keystores</code> directory and then in current working directory or the full path specified in the location parameters.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SslSocketFactoryFactory {

   private static final Logger log = LogManager.getLogger(SslSocketFactoryFactory.class);

   /**
    * Initializes key store from the given location and password.
    *
    * @param keyStoreLocation
    *       Key store location.
    * @param keyStorePassword
    *       Key store password.
    * @return The key store initialized from the provided location and password.
    * @throws GeneralSecurityException
    *       When it was not possible to initialize the key store.
    * @throws IOException
    *       When it was not possible to read the provided location.
    */
   private static KeyStore initKeyStore(final String keyStoreLocation, final String keyStorePassword) throws GeneralSecurityException, IOException {
      final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      try (InputStream is = Utils.locationToUrl(keyStoreLocation, PerfCakeConst.KEYSTORES_DIR_PROPERTY, Utils.determineDefaultLocation("keystores"), "").openStream()) {
         keyStore.load(is, keyStorePassword == null ? null : keyStorePassword.toCharArray());
      }

      return keyStore;
   }

   /**
    * Gets a new SSL socket factory configured with the provided key and trust store.
    *
    * @param keyStore
    *       Key store location.
    * @param keyStorePassword
    *       Key store password.
    * @param trustStore
    *       Trust store location.
    * @param trustStorePassword
    *       Trust store password.
    * @return The new SSL socket factory configured with the provided key and trust store.
    * @throws PerfCakeException
    *       When it was not possible to create the SSL socket factory.
    */
   public static SSLSocketFactory newSslSocketFactory(final String keyStore, final String keyStorePassword, final String trustStore, final String trustStorePassword) throws PerfCakeException {
      return newSslSocketFactory(keyStore, keyStorePassword, trustStore, trustStorePassword, "TLS");
   }

   /**
    * Gets a new SSL socket factory configured with the provided key and trust store.
    *
    * @param keyStore
    *       Key store location.
    * @param keyStorePassword
    *       Key store password.
    * @param trustStore
    *       Trust store location.
    * @param trustStorePassword
    *       Trust store password.
    * @param protocol
    *       Protocol for the factory (TLS, SSL...).
    * @return The new SSL socket factory configured with the provided key and trust store.
    * @throws PerfCakeException
    *       When it was not possible to create the SSL socket factory.
    */
   public static SSLSocketFactory newSslSocketFactory(final String keyStore, final String keyStorePassword, final String trustStore, final String trustStorePassword, final String protocol) throws PerfCakeException {
      KeyStore keyStore_;
      KeyStore trustStore_;
      KeyManagerFactory keyManager = null;
      TrustManagerFactory trustManager = null;

      try {

         if (keyStore != null) {
            if (keyStorePassword == null) {
               log.warn("Empty keystore password.");
            }
            keyStore_ = initKeyStore(keyStore, keyStorePassword);
            keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManager.init(keyStore_, keyStorePassword == null ? null : keyStorePassword.toCharArray());
         }

         if (trustStore != null) {
            if (trustStorePassword == null) {
               log.warn("Empty trust store password.");
            }
            trustStore_ = initKeyStore(trustStore, trustStorePassword);
            trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManager.init(trustStore_);
         }

         final SSLContext ctx = SSLContext.getInstance("TLS");
         ctx.init(keyManager == null ? null : keyManager.getKeyManagers(), trustManager == null ? null : trustManager.getTrustManagers(), null);

         return ctx.getSocketFactory();
      } catch (GeneralSecurityException | IOException e) {
         throw new PerfCakeException("Unable to prepare SSL socket factory: ", e);
      }
   }
}
