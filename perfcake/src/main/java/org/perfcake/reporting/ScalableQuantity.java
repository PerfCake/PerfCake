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

import java.util.Locale;

/**
 * A number with a scalable unit.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public abstract class ScalableQuantity<N extends Number> extends Quantity<N> {

   /**
    * Creates a new scalable quantity.
    *
    * @param number
    *       The value.
    * @param unit
    *       The unit of the value.
    */
   public ScalableQuantity(final N number, final String unit) {
      super(number, unit);
   }

   /**
    * Gets a scale factor of the quantity.
    *
    * For example the decimal numbers have scale factor for prefixes with the value of 1000.
    *
    * @return A scalefactor for the current quantity.
    **/
   protected abstract N getScaleFactor();

   /**
    * Gets a unit prefix for the current value of the quantity.
    *
    * For example in the case of decimal numbers
    * the scale prefix for the value of 1,000 would be <code>k</code> or for the value of 1,000,000 would be <code>M</code>.
    **/
   protected abstract String getScalePrefix(final int power);

   @Override
   public String toString() {
      double valuePan = getNumber().doubleValue();
      final double scaleFactor = getScaleFactor().doubleValue();
      int i = 0;
      while (valuePan >= scaleFactor) {
         valuePan = valuePan / scaleFactor;
         i++;
      }
      return String.format(Locale.US, "%.2f", valuePan) + " " + getScalePrefix(i) + getUnit();
   }
}