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

package org.perfcake.reporting;

/**
 * Represents one discrete performance measurement.
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class Measurement {
   /**
    * For example average response time; average throughoutput. These types are
    * as constants in MeasurementTypes class.
    */
   private String measurementType;

   /**
    * Type of lables used for this Measurement for example "number of messages",
    * "time".
    */
   private String labelType;

   /**
    * Concrete label: "00:12:01" or "34"
    */
   private String label = "";

   /**
    * Value of the measurement. Concrete number represting average throughoutput
    * or whatever measurement type is measured.
    */
   private String value = "";

   public Measurement(String measurementType, String labelType, String label, String value) {
      super();

      if (measurementType == null || "".equals(measurementType.trim())) {
         throw new IllegalArgumentException("Measurement type must be nonempty string! E.g. Response Time");
      }
      if (labelType == null || "".equals(labelType.trim())) {
         throw new IllegalArgumentException("Label type must be nonempty string! E.g. Time");
      }
      if (label == null || "".equals(label.trim())) {
         throw new IllegalArgumentException("Label must be nonempty string! E.g. 00:01:13");
      }
      if (value == null || "".equals(value.trim())) {
         throw new IllegalArgumentException("Value must be nonempty string! E.g. 345");
      }

      this.measurementType = measurementType;
      this.labelType = labelType;
      this.label = label;
      this.value = value;
   }

   public String getMeasurementType() {
      return measurementType;
   }

   public void setMeasurementType(String measurementType) {
      this.measurementType = measurementType;
   }

   public String getLabelType() {
      return labelType;
   }

   public void setLabelType(String labelType) {
      this.labelType = labelType;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return "Measurement{" + "measurementType='" + measurementType + '\'' + ", labelType='" + labelType + '\'' + ", label='" + label + '\'' + ", value='" + value + '\'' + '}';
   }
}
