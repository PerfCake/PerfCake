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
package org.perfcake.reporting.reporter;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.util.Utils;

import io.vertx.core.json.Json;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Reports geolocation of PerfCake by its external IP address. Uses a 3rd party public REST API.
 * Obtains the location just once and keeps it reporting. It does not make sense to configure this reporter to report more than once.
 * As a bonus, it also reports iterations per second in the same way as {@link IterationsPerSecondReporter} does.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class GeolocationReporter extends AbstractReporter {

   /**
    * Our logger.
    */
   private static final Logger log = LogManager.getLogger(GeolocationReporter.class);

   /**
    * URL of the service used to obtain the location.
    */
   private String serviceUrl = "http://ipinfo.io/json";

   /**
    * The actual values obtained from the service.
    */
   private Geolocation geolocation;

   @Override
   protected void doReset() {
      HttpURLConnection connection = null;

      try {
         final URL url = new URL(serviceUrl);
         connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("GET");
         connection.connect();

         final String location = IOUtils.toString(connection.getInputStream(), Utils.getDefaultEncoding());
         geolocation = Json.decodeValue(location, Geolocation.class);
      } catch (IOException e) {
         log.warn("Unable to obtain geolocation from the external service: ", e);
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      // nothing needed
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      final Measurement m = newMeasurement();
      m.set(Measurement.DEFAULT_RESULT, new Quantity<>(1000.0 * runInfo.getIteration() / runInfo.getRunTime(), "iterations/s"));

      m.set("ip", geolocation.ip);
      m.set("hostname", geolocation.hostname);
      m.set("city", geolocation.city);
      m.set("region", geolocation.region);
      m.set("country", geolocation.country);
      m.set("lon", geolocation.lon);
      m.set("lat", geolocation.lat);

      destination.report(m);
   }

   /**
    * Gets the URL of the service used to obtain the location.
    *
    * @return The URL of the service used to obtain the location.
    */
   public String getServiceUrl() {
      return serviceUrl;
   }

   /**
    * Sets the URL of the service used to obtain the location.
    *
    * @param serviceUrl
    *       The URL of the service used to obtain the location.
    * @return Instance of this to support fluent API.
    */
   public GeolocationReporter setServiceUrl(final String serviceUrl) {
      this.serviceUrl = serviceUrl;
      return this;
   }

   /**
    * Carries deserialized JSON data.
    */
   private static class Geolocation {
      private String ip;
      private String hostname;
      private String city;
      private String region;
      private String country;
      private String loc;
      private String org;
      private String postal;
      private String lon;
      private String lat;

      public String getIp() {
         return ip;
      }

      public void setIp(final String ip) {
         this.ip = ip;
      }

      public String getHostname() {
         return hostname;
      }

      public void setHostname(final String hostname) {
         this.hostname = hostname;
      }

      public String getCity() {
         return city;
      }

      public void setCity(final String city) {
         this.city = city;
      }

      public String getRegion() {
         return region;
      }

      public void setRegion(final String region) {
         this.region = region;
      }

      public String getCountry() {
         return country;
      }

      public void setCountry(final String country) {
         this.country = country;
      }

      public String getLoc() {
         return loc;
      }

      public void setLoc(final String loc) {
         this.loc = loc;
         if (loc != null && !"".equals(loc)) {
            final String[] lonlat = loc.split(",");
            if (lonlat.length == 2) {
               this.lat = lonlat[0];
               this.lon = lonlat[1];
            }
         }
      }

      public String getOrg() {
         return org;
      }

      public void setOrg(final String org) {
         this.org = org;
      }

      public String getPostal() {
         return postal;
      }

      public void setPostal(final String postal) {
         this.postal = postal;
      }

      public String toString() {
         return Json.encodePrettily(this);
      }
   }
}
