/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.chart.ChartDestinationHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Creates nice charts from the results.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ChartDestination implements Destination {

   private static final Logger log = LogManager.getLogger(ChartDestination.class);

   private List<String> attributes;

   private Path target = Paths.get("perfcake-chart");

   private String name = "PerfCake Results";

   private String group = "results";

   private String yAxis = "Iterations";

   private String xAxis = "Time";

   private ChartDestinationHelper helper;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#open()
    */
   @Override
   public void open() {
      helper = new ChartDestinationHelper(this);
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#close()
    */
   @Override
   public void close() {
      if (!helper.isSuccessInit()) {
         log.error("Chart destination was not properly initialized, skipping result creation.");
      } else {
         try {
            helper.compileResults();
         } catch (PerfCakeException e) {
            log.error("Unable to compile all collected results in a final report: ", e);
         }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.destinations.Destination#report(org.perfcake.reporting.Measurement)
    */
   @Override
   public void report(final Measurement m) throws ReportingException {
      if (!helper.isSuccessInit()) {
         throw new ReportingException("Chart destination was not properly initialized.");
      }

      helper.appendResult(m);
   }

   public String getTarget() {
      return target.toString();
   }

   public void setTarget(final String target) {
      this.target = Paths.get(target);
   }

   public Path getTargetAsPath() {
      return target;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getGroup() {
      return group;
   }

   public void setGroup(final String group) {
      this.group = group;
   }

   public String getYAxis() {
      return yAxis;
   }

   public void setYAxis(final String yAxis) {
      this.yAxis = yAxis;
   }

   public String getXAxis() {
      return xAxis;
   }

   public void setXAxis(final String xAxis) {
      this.xAxis = xAxis;
   }

   public String getAttributes() {
      return StringUtils.join(attributes, ",");
   }

   public void setAttributes(final String attributes) {
      this.attributes = Arrays.asList(attributes.split("\\s*,\\s*"));
   }

   public List<String> getAttributesAsList() {
      return attributes;
   }
}
