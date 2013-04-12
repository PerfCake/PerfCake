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

package org.perfcake.util;

import java.util.Collection;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.perfcake.message.generator.types.MemoryRecord;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class LinearRegression {
   public static float computeMemoryUsedRegressionTrend(Collection<MemoryRecord> data) {
      SimpleRegression reg = new SimpleRegression();
      for (MemoryRecord record : data) {
         reg.addData(record.getTimestamp(), record.getTotal() - record.getFree());
      }
      return (float) reg.getSlope();
   }
}
