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
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.StringUtil;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ElasticsearchDestination implements Destination {

   private final static Logger log = LogManager.getLogger(ElasticsearchDestination.class);

   private String serverUri;

   private String index = "perfcake";

   private String type = "results";

   private String tags = "";

   private String userName = null;

   private String password = "";

   private boolean configureMapping = true;

   private long startTime;

   private JestClient jest;

   private JsonArray tagsArray = new JsonArray();

   private ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());

   @Override
   public void open() {
      startTime = Long.getLong(System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY));

      final List<String> serverUris = Arrays.asList(serverUri.split(",")).stream().map(StringUtil::trim).collect(Collectors.toList());
      final HttpClientConfig.Builder builder = new HttpClientConfig.Builder(serverUris);

      Arrays.asList(tags.split(",")).stream().forEach(tagsArray::add);

      builder.multiThreaded(true);

      if (userName != null) {
         builder.defaultCredentials(userName, password);
      }

      final JestClientFactory factory = new JestClientFactory();
      factory.setHttpClientConfig(builder.build());
      jest = factory.getObject();
   }

   @Override
   public void close() {
      pool.shutdown();
      try {
         pool.awaitTermination(30, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         log.warn("Unable to report all results to Elasticsearch: ", e);
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

      pool.submit(() -> {
         final Index indexInstance = new Index.Builder(jsonObject.toString()).index(index).type(type).build();

         try {
            jest.execute(indexInstance);
         } catch (IOException e) {
            log.error("Unable to write results to Elasticsearch: ", e);
         }
      });
   }

   public String getServerUri() {
      return serverUri;
   }

   public void setServerUri(final String serverUri) {
      this.serverUri = serverUri;
   }

   public String getIndex() {
      return index;
   }

   public void setIndex(final String index) {
      this.index = index;
   }

   public String getType() {
      return type;
   }

   public void setType(final String type) {
      this.type = type;
   }

   public String getTags() {
      return tags;
   }

   public void setTags(final String tags) {
      this.tags = tags;
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

   public boolean isConfigureMapping() {
      return configureMapping;
   }

   public void setConfigureMapping(final boolean configureMapping) {
      this.configureMapping = configureMapping;
   }
}
