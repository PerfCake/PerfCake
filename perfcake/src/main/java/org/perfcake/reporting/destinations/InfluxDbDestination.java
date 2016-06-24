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
package org.perfcake.reporting.destinations;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.SslSocketFactoryFactory;

import com.google.gson.JsonArray;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import retrofit.client.OkClient;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class InfluxDbDestination implements Destination {

   private static final Logger log = LogManager.getLogger(InfluxDbDestination.class);

   private String serverUrl = "";

   private String database = "perfcake";

   private String measurement = "results";

   private String userName = "admin";

   private String password = "admin";

   private String tags = "";

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

   /**
    * Initialized SSL factory.
    */
   private SSLSocketFactory sslFactory;

   /**
    * Cached array with tags.
    */
   private JsonArray tagsArray = new JsonArray();

   private InfluxDB influxDb = null;

   @Override
   public void open() {
      Arrays.asList(tags.split(",")).stream().forEach(tagsArray::add);

      try {
         if ((keyStore != null && !"".equals(keyStore)) || (trustStore != null && !"".equals(trustStore))) {
            sslFactory = SslSocketFactoryFactory.newSslSocketFactory(keyStore, keyStorePassword, trustStore, trustStorePassword);
         }
      } catch (PerfCakeException e) {
         log.warn("Unable to initialize SSL socket factory: ", e);
      }

      try {
         if (sslFactory != null) {
            final OkHttpClient client = new OkHttpClient();
            client.setSslSocketFactory(sslFactory);
            client.setHostnameVerifier((hostname, session) -> true);
            influxDb = InfluxDBFactory.connect(serverUrl, userName, password, new OkClient(client));
         } else {
            influxDb = InfluxDBFactory.connect(serverUrl, userName, password);
         }
         influxDb.createDatabase(database);
         influxDb.enableBatch(100, 500, TimeUnit.MILLISECONDS);
      } catch (RuntimeException rte) {
         influxDb = null;
         log.error("Unable to connect to InfluxDb: ", rte);
      }
   }

   @Override
   public void close() {
      if (influxDb != null) {
         influxDb.disableBatch(); // flushes batch
      }
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      if (influxDb == null) {
         throw new ReportingException("Not connected to InfluxDb.");
      }

      Point.Builder pBuilder = Point.measurement(this.measurement).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);

      pBuilder.addField(PeriodType.TIME.toString().toLowerCase(), measurement.getTime());
      pBuilder.addField(PeriodType.ITERATION.toString().toLowerCase(), measurement.getIteration());
      pBuilder.addField(PeriodType.PERCENTAGE.toString().toLowerCase(), measurement.getPercentage());
      pBuilder.addField(PerfCakeConst.TAGS_TAG, tagsArray.toString());

      measurement.getAll().forEach((k, v) -> {
         if (v instanceof Number) {
            pBuilder.addField(k, (Number) v);
         } else if (v instanceof Quantity) {
            pBuilder.addField(k, ((Quantity) v).getNumber());
         } else {
            pBuilder.addField(k, v.toString());
         }
      });

      influxDb.write(database, "default", pBuilder.build());
   }

   public String getServerUrl() {
      return serverUrl;
   }

   public void setServerUrl(final String serverUrl) {
      this.serverUrl = serverUrl;
   }

   public String getDatabase() {
      return database;
   }

   public void setDatabase(final String database) {
      this.database = database;
   }

   public String getMeasurement() {
      return measurement;
   }

   public void setMeasurement(final String measurement) {
      this.measurement = measurement;
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

   public String getTags() {
      return tags;
   }

   public void setTags(final String tags) {
      this.tags = tags;
   }

   public String getKeyStore() {
      return keyStore;
   }

   public void setKeyStore(final String keyStore) {
      this.keyStore = keyStore;
   }

   public String getKeyStorePassword() {
      return keyStorePassword;
   }

   public void setKeyStorePassword(final String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
   }

   public String getTrustStore() {
      return trustStore;
   }

   public void setTrustStore(final String trustStore) {
      this.trustStore = trustStore;
   }

   public String getTrustStorePassword() {
      return trustStorePassword;
   }

   public void setTrustStorePassword(final String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
   }
}
