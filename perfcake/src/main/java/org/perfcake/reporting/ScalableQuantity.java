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
    * <p>The base power for the quantity value.</p>
    *
    * <p>For example in case of decimal numbers, when we want to insert 123 ms, we don't have to re-compute the value to the base unit power
    * (and pass it as <code>0.123 s</code>. With the base power we can pass the number value as is <code>123</code> and pass it with
    * the base power of <code>-1</code>.</p>
    */
   private int basePower = 0;

   /**
    * Creates a new scalable quantity.
    *
    * @param number
    *       The value.
    * @param unit
    *       The unit of the value.
    */
   public ScalableQuantity(final N number, final String unit) {
      this(number, 0, unit);
   }

   /**
    * <p>Creates a new scalable quantity with the specified base power.</p>
    *
    * <p>For example in case of decimal numbers, when we want to insert 123 ms, we don't have to re-compute the value to the base unit power
    * (and pass it as <code>0.123 s</code>. With the base power we can pass the number value as is <code>123</code> and pass it with
    * the base power of <code>-1</code>.</p>
    *
    * @param number
    *       The value.
    * @param basePower
    *       The base power of the value.
    * @param unit
    *       The unit of the value.
    */
   public ScalableQuantity(final N number, final int basePower, final String unit) {
      super(number, unit);
      if (basePower < getMinPower() || basePower > getMaxPower()) {
         throw new IllegalArgumentException("Base power " + basePower + " is out of range. It should be between " + getMinPower() + " and " + getMaxPower() + ".");
      }
      this.basePower = basePower;
   }

   /**
    * <p>Gets a scale factor of the quantity.</p>
    *
    * <p>For example the decimal numbers have scale factor for prefixes with the value of 1000.</p>
    *
    * @return A scalefactor for the current quantity.
    **/
   protected abstract N getScaleFactor();

   /**
    * Do the actual mapping of the scaling power to the unit prefix. After the power value was bounded to the
    * range of {@link #getMinPower()} to {@link #getMaxPower()}.
    *
    * @param power
    *       Power of the base quantity
    * @return The scale prefix for the number.
    **/
   protected abstract String getBoundedScalePrefix(final int power);

   /**
    * Gets a minimal power for which the scalable quantity has a unit prefix.
    *
    * @return The minimal power value.
    **/
   protected abstract int getMinPower();

   /**
    * Gets a max power for which the scalable quantity has a unit prefix.
    *
    * @return The minimal power value.
    **/
   protected abstract int getMaxPower();

   /**
    * <p>Gets the base power for the quantity value.</p>
    *
    * <p>For example in case of decimal numbers, when we want to insert 123 ms, we don't have to re-compute the value to the base unit power
    * (and pass it as <code>0.123 s</code>. With the base power we can pass the number value as is <code>123</code> and pass it with
    * the base power of <code>-1</code>.</p>
    *
    * @return The base power value.
    */
   protected int getBasePower() {
      return basePower;
   }

   /**
    * <p>Gets a unit prefix for the current value of the quantity.</p>
    *
    * <p>For example in the case of decimal numbers the scale prefix for the value of 1,000
    * would be <code>k</code> or for the value of 1,000,000 would be <code>M</code> which needs
    * to be passed as 1 and 2 respectively. The result is the prefix for the number of 10^(power * 3).</p>
    *
    * @param power
    *       Scale power of the base quantity I.e. 10^(power * 3) for decimal numbers.
    * @return The scale prefix for the number. E.g. 10^(power * 3), -2 = μ, -1 = m, 1 = k, 2 = M, 3 = G etc. for decimal numbers.
    **/
   protected String getScalePrefix(final int power) {
      int boundedPower;
      if (power < getMinPower()) {
         boundedPower = getMinPower();
      } else if (power > getMaxPower()) {
         boundedPower = getMaxPower();
      } else {
         boundedPower = power;
      }
      return getBoundedScalePrefix(boundedPower);
   }

   @Override
   public String toString() {
      double valuePan = getNumber().doubleValue();
      final double scaleFactor = getScaleFactor().doubleValue();
      int i = getBasePower();
      if (valuePan > 1.0) { // need to scale the value down
         while (valuePan >= scaleFactor && i < getMaxPower()) {
            valuePan = valuePan / scaleFactor;
            i++;
         }
      } else {
         while (valuePan < 1.0 && i > getMinPower()) {
            valuePan = valuePan * scaleFactor;
            i--;
         }
      }
      return String.format(Locale.US, "%.2f", valuePan) + " " + getScalePrefix(i) + getUnit();
   }
}
