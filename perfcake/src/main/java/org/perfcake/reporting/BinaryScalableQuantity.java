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
 * A number with a unit scalable by binary system prefixes.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class BinaryScalableQuantity extends ScalableQuantity<Long> {

   public BinaryScalableQuantity(final Long number, final String unit) {
      super(number, unit);
   }

   public BinaryScalableQuantity(final Long number, final int basePower, final String unit) {
      super(number, basePower, unit);
   }

   @Override
   protected Long getScaleFactor() {
      return 1024L;
   }

   @Override
   protected final int getMinPower() {
      return 0;
   }

   @Override
   protected final int getMaxPower() {
      return 8;
   }

   @Override
   protected String getBoundedScalePrefix(final int power) {
      switch (power) {
         case 0:
            return "";
         case 1:
            return "Ki";
         case 2:
            return "Mi";
         case 3:
            return "Gi";
         case 4:
            return "Ti";
         case 5:
            return "Pi";
         case 6:
            return "Ei";
         case 7:
            return "Zi";
         case 8:
            return "Yi";
         default:
            // should not happen ever
            throw new IllegalArgumentException("Unable to determine prefix for the power " + power);
      }
   }
}
