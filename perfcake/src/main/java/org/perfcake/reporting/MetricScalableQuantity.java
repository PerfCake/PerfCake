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
package org.perfcake.reporting;

/**
 * A number with a unit scalable by metric system prefixes.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class MetricScalableQuantity extends ScalableQuantity<Double> {

   public MetricScalableQuantity(final Double number, final String unit) {
      super(number, unit);
   }

   @Override
   public Double getScaleFactor() {
      return 1000.0;
   }

   @Override
   public String getScalePrefix(final int power) {
      switch (power) {
         case 0:
            return "";
         case 1:
            return "k";
         case 2:
            return "M";
         case 3:
            return "G";
         case 4:
            return "T";
         case 5:
            return "P";
         case 6:
            return "E";
         case 7:
            return "Z";
         default:
            return "Y";
      }
   }
}
