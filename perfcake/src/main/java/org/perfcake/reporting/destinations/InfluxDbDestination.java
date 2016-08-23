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
import org.perfcake.util.StringUtil;
import org.perfcake.util.properties.MandatoryProperty;

import com.google.gson.JsonArray;
import com.squareup.okhttp.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import retrofit.client.OkClient;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;

/**
 * Writes the resulting data to InfluxDb using a simple HTTP REST client.
 * The reported data have information about the test progress (time in milliseconds since start, percentage and iteration),
 * real time of each result, and the complete results map. Quantities are stored without their unit.
 * Supports SSL connection. The database is by default created on connection.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class InfluxDbDestination extends AbstractDestination {

   private static final Logger log = LogManager.getLogger(InfluxDbDestination.class);

   /**
    * InfluxDb server including protocol and port number. Supports SSL.
    */
   @MandatoryProperty
   private String serverUrl = "";

   /**
    * Name of InfluxDb database.
    */
   private String database = "perfcake";

   /**
    * Creates the database when connected. Requires admin privileges in InfluxDb.
    */
   private boolean createDatabase = true;

   /**
    * Name of the measurement in InfluxDb, serves as a database table.
    */
   private String measurement = "results";

   /**
    * InfluxDb user name.
    */
   @MandatoryProperty
   private String userName = "admin";

   /**
    * InfluxDb password.
    */
   @MandatoryProperty
   private String password = "admin";

   /**
    * Comma separated list of tags to be added to results.
    */
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
   private SSLSocketFactory sslFactory = null;

   /**
    * Cached array with tags.
    */
   private JsonArray tagsArray = new JsonArray();

   /**
    * InfluxDb client.
    */
   private InfluxDB influxDb = null;

   @Override
   public void open() {
      Arrays.stream(tags.split(",")).map(StringUtil::trim).forEach(tagsArray::add);

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
         if (createDatabase) {
            influxDb.createDatabase(database);
         }
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

   /**
    * Gets InfluxDb server including protocol and port number. Supports SSL.
    *
    * @return InfluxDb server including protocol and port number.
    */
   public String getServerUrl() {
      return serverUrl;
   }

   /**
    * Sets InfluxDb server including protocol and port number. Supports SSL.
    *
    * @param serverUrl
    *       InfluxDb server including protocol and port number.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setServerUrl(final String serverUrl) {
      this.serverUrl = serverUrl;
      return this;
   }

   /**
    * Gets the name of InfluxDb database.
    *
    * @return The name of InfluxDb database.
    */
   public String getDatabase() {
      return database;
   }

   /**
    * Sets the name of InfluxDb database.
    *
    * @param database
    *       The name of InfluxDb database.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setDatabase(final String database) {
      this.database = database;
      return this;
   }

   /**
    * Should the database be created upon connecting to InfluxDb? Defaults to true.
    *
    * @return True if and only if the database should be created when connected to InfluxDb.
    */
   public boolean isCreateDatabase() {
      return createDatabase;
   }

   /**
    * Sets whether the database should be created upon connecting to InfluxDb. Dafaults to true.
    *
    * @param createDatabase
    *       True if and only if the database should be created when connected to InfluxDb.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setCreateDatabase(final boolean createDatabase) {
      this.createDatabase = createDatabase;
      return this;
   }

   /**
    * Gets the name of the measurement in InfluxDb, serves as a database table.
    *
    * @return The name of the measurement in InfluxDb, serves as a database table.
    */
   public String getMeasurement() {
      return measurement;
   }

   /**
    * Sets the name of the measurement in InfluxDb, serves as a database table.
    *
    * @param measurement
    *       The name of the measurement in InfluxDb, serves as a database table.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setMeasurement(final String measurement) {
      this.measurement = measurement;
      return this;
   }

   /**
    * Gets the name of InfluxDb user.
    *
    * @return The name of InfluxDb user.
    */
   public String getUserName() {
      return userName;
   }

   /**
    * Sets the name of InfluxDb user.
    *
    * @param userName
    *       The name of InfluxDb user.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setUserName(final String userName) {
      this.userName = userName;
      return this;
   }

   /**
    * Gets the InfluxDb user password.
    *
    * @return The InfluxDb user password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the InfluxDb user password.
    *
    * @param password
    *       The InfluxDb user password.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setPassword(final String password) {
      this.password = password;
      return this;
   }

   /**
    * Gets a comma separated list of tags to be added to results.
    *
    * @return The comma separated list of tags to be added to results.
    */
   public String getTags() {
      return tags;
   }

   /**
    * Sets a comma separated list of tags to be added to results.
    *
    * @param tags
    *       The comma separated list of tags to be added to results.
    * @return Instance of this to support fluent API.
    */
   public InfluxDbDestination setTags(final String tags) {
      this.tags = tags;
      return this;
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
   public InfluxDbDestination setKeyStore(final String keyStore) {
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
   public InfluxDbDestination setKeyStorePassword(final String keyStorePassword) {
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
   public InfluxDbDestination setTrustStore(final String trustStore) {
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
   public InfluxDbDestination setTrustStorePassword(final String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
      return this;
   }
}
