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
package org.perfcake.reporting.destination.anomalyDetection;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.Reporter;

import java.util.List;

/**
 * An algorithm for detecting suspicious results in the list of measured values from the performance test
 * and identifying a performance issue.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public interface Heuristics {

   /**
    * Performs the heuristics with the given data set.
    * @param dataSet
    *       A list of measured results from the performance test.
    */
   void run(List<Measurement> dataSet);

   /**
    * Analyzes result of the heuristics for the purpose of detecting anomalies.
    * @return evaluation string (identified performance problem if any)
    */
   String analyzeResults();
}
