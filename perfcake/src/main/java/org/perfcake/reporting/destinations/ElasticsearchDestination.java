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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;

/**
 * Writes the resulting data to Elasticsearch using a simple HTTP REST client.
 * The reported data have information about the test progress (time in milliseconds since start, percentage and iteration),
 * real time of each result, and the complete results map. Quantities are stored without their unit.
 * To properly search through the data, we need to set the mapping (to be able to interpret time as time).
 * However, this needs to be done just once for each index and type.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ElasticsearchDestination implements Destination {

   /**
    * Our logger.
    */
   private final static Logger log = LogManager.getLogger(ElasticsearchDestination.class);

   /**
    * Comma separated list of Elastisearch servers including protocol and port number.
    */
   private String serverUri;

   /**
    * Elasticsearch index name.
    */
   private String index = "perfcake";

   /**
    * Elasticsearch type name.
    */
   private String type = "results";

   /**
    * Comma separated list of tags to be added to results.
    */
   private String tags = "";

   /**
    * Elasticsearch user name.
    */
   private String userName = null;

   /**
    * Elasticsearch password.
    */
   private String password = "";

   /**
    * Elasticsearch client timeout.
    */
   private int timeout = 5000;

   /**
    * To properly search through the data, we need to set the mapping. However, this needs to be done just once for each index and type.
    */
   private boolean configureMapping = true;

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
    * Initialized SSL context.
    */
   private SSLContext sslContext = null;

   /**
    * Time when the test was started.
    */
   private long startTime;

   /**
    * Elasticsearch client.
    */
   private JestClient jest;

   /**
    * Requests with reported data.
    */
   private ThreadPoolExecutor elasticRequests = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());

   /**
    * Cached array with tags.
    */
   private JsonArray tagsArray = new JsonArray();

   @Override
   public void open() {
      startTime = Long.getLong(PerfCakeConst.TIMESTAMP_PROPERTY);

      try {
         if ((keyStore != null && !"".equals(keyStore)) || (trustStore != null && !"".equals(trustStore))) {
            sslContext = SslSocketFactoryFactory.newSslContext(keyStore, keyStorePassword, trustStore, trustStorePassword);
         }
      } catch (PerfCakeException e) {
         log.warn("Unable to initialize SSL socket factory: ", e);
      }

      final List<String> serverUris = Arrays.asList(serverUri.split(",")).stream().map(StringUtil::trim).collect(Collectors.toList());
      final HttpClientConfig.Builder builder = new HttpClientConfig.Builder(serverUris);

      if (sslContext != null) {
         builder.sslSocketFactory(new SSLConnectionSocketFactory(sslContext));
      }

      Arrays.asList(tags.split(",")).stream().map(StringUtil::trim).forEach(tagsArray::add);

      builder.multiThreaded(true);

      builder.connTimeout(timeout);
      builder.readTimeout(timeout);
      builder.maxTotalConnection(1);

      if (userName != null) {
         builder.defaultCredentials(userName, password);
      }

      final JestClientFactory factory = new JestClientFactory();
      factory.setHttpClientConfig(builder.build());
      jest = factory.getObject();

      if (configureMapping) {
         final String mappings =
               "{ \"mappings\": {"
                     + " \"" + type + "\": {"
                     + "\"properties\" : { "
                     + "\"" + PeriodType.TIME.toString().toLowerCase() + "\" : {"
                     + "\"type\" : \"date\", "
                     + "\"format\" : \"epoch_millis\""
                     + "}, "
                     + "\"" + PerfCakeConst.REAL_TIME_TAG + "\" : {"
                     + "\"type\" : \"date\", "
                     + "\"format\" : \"epoch_millis\""
                     + "} "
                     + "} "
                     + "} "
                     + "} "
                     + "}";
         try {
            final JestResult result = jest.execute(new CreateIndex.Builder(index).settings(mappings).build());
            if (result.isSucceeded()) {
               log.info("Correctly configured mapping.");
            } else {
               if (result.getErrorMessage().contains("index_already_exists")) {
                  log.warn("Index already exists, cannot re-configure mapping.");
               } else {
                  throw new IOException(result.getErrorMessage());
               }
            }
         } catch (IOException e) {
            log.warn("Unable to configure mapping: ", e);
         }
      }
   }

   @Override
   public void close() {
      elasticRequests.shutdown();
      try {
         if (elasticRequests.getQueue().size() > 0 || elasticRequests.getActiveCount() > 0) {
            log.info("Waiting to send all results to Elasticsearch...");
         }
         elasticRequests.awaitTermination(30, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         log.error("Could not write all results to Elasticsearch: ", e);
      }

      jest.shutdownClient();
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      final JsonObject jsonObject = new JsonObject();

      jsonObject.addProperty(PeriodType.ITERATION.toString().toLowerCase(), measurement.getIteration());
      jsonObject.addProperty(PeriodType.TIME.toString().toLowerCase(), measurement.getTime());
      jsonObject.addProperty(PeriodType.PERCENTAGE.toString().toLowerCase(), measurement.getPercentage());
      jsonObject.addProperty(PerfCakeConst.REAL_TIME_TAG, startTime + measurement.getTime());
      jsonObject.add(PerfCakeConst.TAGS_TAG, tagsArray);

      measurement.getAll().forEach((k, v) -> {
         if (v instanceof Number) {
            jsonObject.addProperty(k, (Number) v);
         } else if (v instanceof Quantity) {
            jsonObject.addProperty(k, ((Quantity) v).getNumber());
         } else {
            jsonObject.addProperty(k, v.toString());
         }
      });

      final Index indexInstance = new Index.Builder(jsonObject.toString()).index(index).type(type).build();

      // built-in async client constantly timeouts, it seems to ignore timeout setting
      elasticRequests.submit(() -> {
         try {
            jest.execute(indexInstance);
         } catch (IOException ioe) {
            log.error("Unable to write results to Elasticsearch: ", ioe);
         }
      });
   }

   /**
    * Gets the comma separated list of Elasticsearch servers including protocol and port number.
    *
    * @return The comma separated list of Elasticsearch servers including protocol and port number.
    */
   public String getServerUri() {
      return serverUri;
   }

   /**
    * Sets the comma separated list of Elasticsearch servers including protocol and port number.
    *
    * @param serverUri
    *       The comma separated list of Elasticsearch servers including protocol and port number.
    */
   public void setServerUri(final String serverUri) {
      this.serverUri = serverUri;
   }

   /**
    * Gets the Elasticsearch index name.
    *
    * @return The Elasticsearch index name.
    */
   public String getIndex() {
      return index;
   }

   /**
    * Sets the Elasticsearch index name.
    *
    * @param index
    *       The Elasticsearch index name.
    */
   public void setIndex(final String index) {
      this.index = index;
   }

   /**
    * Gets the Elasticsearch type name.
    *
    * @return the Elasticsearch type name.
    */
   public String getType() {
      return type;
   }

   /**
    * Sets the Elasticsearch type name.
    *
    * @param type
    *       the Elasticsearch type name.
    */
   public void setType(final String type) {
      this.type = type;
   }

   /**
    * Gets the comma separated list of tags to be added to results.
    *
    * @return The comma separated list of tags to be added to results.
    */
   public String getTags() {
      return tags;
   }

   /**
    * Sets the comma separated list of tags to be added to results.
    *
    * @param tags
    *       The comma separated list of tags to be added to results.
    */
   public void setTags(final String tags) {
      this.tags = tags;
   }

   /**
    * Gets the Elasticsearch user name.
    *
    * @return The Elasticsearch user name.
    */
   public String getUserName() {
      return userName;
   }

   /**
    * Sets the Elasticsearch user name.
    *
    * @param userName
    *       The Elasticsearch user name.
    */
   public void setUserName(final String userName) {
      this.userName = userName;
   }

   /**
    * Gets the Elasticsearch password.
    *
    * @return The Elasticsearch password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the Elasticsearch password.
    *
    * @param password
    *       The Elasticsearch password.
    */
   public void setPassword(final String password) {
      this.password = password;
   }

   /**
    * Gets whether the mapping of data should be configured prior to writing. To properly search through the data,
    * we need to set the mapping. However, this needs to be done just once for each index and type.
    *
    * @return True if and only if the mapping will be configured prior to writing.
    */
   public boolean isConfigureMapping() {
      return configureMapping;
   }

   /**
    * Sets whether the mapping of data should be configured prior to writing. To properly search through the data,
    * we need to set the mapping. However, this needs to be done just once for each index and type.
    *
    * @param configureMapping
    *       True if the mapping should be configured prior to writing.
    */
   public void setConfigureMapping(final boolean configureMapping) {
      this.configureMapping = configureMapping;
   }

   /**
    * Gets the Elasticsearch client timeout.
    *
    * @return The Elasticsearch client timeout.
    */
   public int getTimeout() {
      return timeout;
   }

   /**
    * Sets the Elasticsearch client timeout.
    *
    * @param timeout
    *       The Elasticsearch client timeout.
    */
   public void setTimeout(final int timeout) {
      this.timeout = timeout;
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
    */
   public void setKeyStore(final String keyStore) {
      this.keyStore = keyStore;
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
    */
   public void setKeyStorePassword(final String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
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
    */
   public void setTrustStore(final String trustStore) {
      this.trustStore = trustStore;
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
    */
   public void setTrustStorePassword(final String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
   }
}
