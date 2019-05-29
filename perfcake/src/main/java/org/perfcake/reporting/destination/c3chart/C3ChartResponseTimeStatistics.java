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

import java.util.ArrayList;
import java.util.List;

/**
 * Results analysis table.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class C3ChartResponseTimeStatistics implements C3ChartResultsAnalysis {

   /**
    * Labels that should be shown in the results analysis part.
    */
   private List<String> raLabelList;

   /**
    * Values that should be shown in the results analysis part.
    */
   private List<String> raValueList;

   /**
    * Evaluations that should be shown in the results analysis part.
    */
   private List<String> raEvaluationList;

   /**
    * Labels of statistics that should be shown in the results analysis part.
    */
   private List<String> statParamLabelList;

   /**
    * Values of statistics that should be shown in the results analysis part.
    */
   private List<String> statParamValueList;

   C3ChartResponseTimeStatistics() {
      this.raLabelList = new ArrayList<>();
      this.raLabelList.add("Threshold");
      this.raLabelList.add("Average");
      this.raLabelList.add("95th percentile");
      this.raLabelList.add("99th percentile");
      this.raLabelList.add("Degradation");
      this.raLabelList.add("Traffic Spike");
      this.raLabelList.add("Regular Spikes");

      this.raValueList = new ArrayList<>();

      this.raEvaluationList = new ArrayList<>();

      this.statParamLabelList = new ArrayList<>();
      this.statParamLabelList.add("Intercept");
      this.statParamLabelList.add("Slope");
      this.statParamLabelList.add("R");
      this.statParamLabelList.add("R square");
      this.statParamLabelList.add("Significance");
      this.statParamLabelList.add("Variance");

      this.statParamValueList = new ArrayList<>();
   }

   /**
    * Gets the list of labels of performed analysis.
    *
    * @return The list of labels.
    */
   @Override
   public List<String> getRaLabelList() {
      return raLabelList;
   }

   /**
    * Sets the list of labels of performed analysis.
    *
    * @param raLabelList
    *       The list of labels.
    */
   public void setRaLabelList(List<String> raLabelList) {
      this.raLabelList = raLabelList;
   }

   /**
    * Gets the list of values of performed analysis.
    *
    * @return The list of values.
    */
   @Override
   public List<String> getRaValueList() {
      return raValueList;
   }

   /**
    * Sets the list of values of performed analysis.
    *
    * @param raValueList
    *       The list of values.
    */
   public void setRaValueList(List<String> raValueList) {
      this.raValueList = raValueList;
   }

   /**
    * Gets the list of evaluations of performed analysis.
    *
    * @return The list of evaluations.
    */
   @Override
   public List<String> getRaEvaluationList() {
      return raEvaluationList;
   }

   /**
    * Sets the list of evaluations of performed analysis.
    *
    * @param raEvaluationList
    *       The list of evaluations.
    */
   public void setRaEvaluationList(List<String> raEvaluationList) {
      this.raEvaluationList = raEvaluationList;
   }

   /**
    * Gets the list of statistics labels that should be shown in the results analysis part.
    *
    * @return The list of labels.
    */
   @Override
   public List<String> getStatParamLabelList() {
      return statParamLabelList;
   }

   /**
    * Sets the list of statistics labels that should be shown in the results analysis part.
    *
    * @param statParamLabelList
    *       The list of labels.
    */
   public void setStatParamLabelList(List<String> statParamLabelList) {
      this.statParamLabelList = statParamLabelList;
   }

   /**
    * Gets the list of statistics values that should be shown in the results analysis part.
    *
    * @return The list of values.
    */
   @Override
   public List<String> getStatParamValueList() {
      return statParamValueList;
   }

   /**
    * Sets the list of statistics values that should be shown in the results analysis part.
    *
    * @param statParamValueList
    *       The list of values.
    */
   public void setStatParamValueList(List<String> statParamValueList) {
      this.statParamValueList = statParamValueList;
   }

   @Override
   public void addRaValueList(CrystalDestination crystalDestination) {
      this.raValueList.add(String.valueOf(crystalDestination.getThreshold().intValue()) + " ms");
      this.raValueList.add(String.valueOf(crystalDestination.getStats().getMean().intValue()) + " ms");
      this.raValueList.add(String.valueOf(crystalDestination.getStats().getPercentile95().intValue()) + " ms");
      this.raValueList.add(String.valueOf(crystalDestination.getStats().getPercentile99().intValue()) + " ms");
      this.raValueList.add(String.valueOf(crystalDestination.getRa().getDegPerc()) + " %");
      this.raValueList.add(String.valueOf(crystalDestination.getRa().getTrafSPerc()) + " %");
      this.raValueList.add(String.valueOf(crystalDestination.getRa().getRegsPerc()) + " %");
   }

   @Override
   public void addRaEvaluationList(CrystalDestination crystalDestination) {
      addColoredEvaluation(crystalDestination.getStats().isThresholdResultExceeded(), this.raEvaluationList);
      addColoredEvaluation(crystalDestination.getStats().isThresholdAverageExceeded(), this.raEvaluationList);
      addColoredEvaluation(crystalDestination.getStats().isThreshold95thExceeded(), this.raEvaluationList);
      addColoredEvaluation(crystalDestination.getStats().isThreshold99thExceeded(), this.raEvaluationList);
      addColoredEvaluation(crystalDestination.getRa().isDegradation(), this.raEvaluationList);
      addColoredEvaluation(crystalDestination.getRa().isTrafficSpike(), this.raEvaluationList);
      addColoredEvaluation(crystalDestination.getRa().isRegularSpikes(), this.raEvaluationList);
   }

   @Override
   public void addStatValueList(CrystalDestination crystalDestination) {
      this.statParamValueList.add(String.format("%.2f", crystalDestination.getRa().getIntercept()));
      this.statParamValueList.add(String.format("%.4f", crystalDestination.getRa().getSlope()));
      this.statParamValueList.add(String.format("%.4f", crystalDestination.getRa().getR()));
      this.statParamValueList.add(String.format("%.8f", crystalDestination.getRa().getrSquare()));
      this.statParamValueList.add(String.format("%.8f", crystalDestination.getRa().getP()));
      this.statParamValueList.add(String.format("%.4f", crystalDestination.getStats().getVariance()));
   }

   /**
    * Adds a new evaluation item into the given list.
    *
    * @param thresholdExceeded
    *       The boolean value indicating the exceeded threshold.
    * @param list
    *       The list of evaluations.
    */
   void addEvaluation(boolean thresholdExceeded, List<String> list) {
      if (thresholdExceeded) {
         list.add("<span class=\"glyphicon glyphicon-remove\">");
      } else {
         list.add("<span class=\"glyphicon glyphicon-ok\">");
      }
   }

   /**
    * Adds a new colored evaluation item into the given list.
    *
    * @param thresholdExceeded
    *       The boolean value indicating the exceeded threshold.
    * @param list
    *       The list of evaluations.
    */
   private void addColoredEvaluation(boolean thresholdExceeded, List<String> list) {
      if (thresholdExceeded) {
         list.add("<span class=\"glyphicon glyphicon-remove\" style=\"color: rgb(221, 72, 20);\">");
      } else {
         list.add("<span class=\"glyphicon glyphicon-ok\" style=\"color: green;\">");
      }
   }

}