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

import com.google.gson.JsonArray;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class InfluxDbDestination implements Destination {

   private String serverUrl = "";

   private String database = "perfcake";

   private String measurement = "results";

   private String userName = "admin";

   private String password = "admin";

   private String tags = "";

   /**
    * Cached array with tags.
    */
   private JsonArray tagsArray = new JsonArray();

   private InfluxDB influxDb;

   @Override
   public void open() {
      Arrays.asList(tags.split(",")).stream().forEach(tagsArray::add);

      influxDb = InfluxDBFactory.connect(serverUrl, userName, password);
      influxDb.createDatabase(database);
      influxDb.enableBatch(100, 500, TimeUnit.MILLISECONDS);
   }

   @Override
   public void close() {
      influxDb.disableBatch(); // flushes batch
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
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
}
