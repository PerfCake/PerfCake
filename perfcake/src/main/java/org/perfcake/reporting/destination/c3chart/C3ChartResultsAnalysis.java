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
package org.perfcake.reporting.destination.c3chart;

import org.perfcake.reporting.destination.CrystalDestination;

import java.util.List;

/**
 * Results analysis table.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public interface C3ChartResultsAnalysis {

   void addRaValueList(CrystalDestination crystalDestination);

   void addRaEvaluationList(CrystalDestination crystalDestination);

   void addStatValueList(CrystalDestination crystalDestination);

   List<String> getRaLabelList();

   List<String> getRaValueList();

   List<String> getRaEvaluationList();

   List<String> getStatParamLabelList();

   List<String> getStatParamValueList();

}