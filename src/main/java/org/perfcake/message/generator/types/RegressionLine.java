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

package org.perfcake.message.generator.types;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class RegressionLine {

   private float a, b;

   public RegressionLine(float a, float b) {
      this.a = a;
      this.b = b;
   }

   public float getA() {
      return a;
   }

   public void setA(long a) {
      this.a = a;
   }

   public float getB() {
      return b;
   }

   public void setB(long b) {
      this.b = b;
   }

   @Override
   public String toString() {
      return a + " * x +" + b;
   }

   public static final RegressionLine NULL = new RegressionLine(0, 0);
}
