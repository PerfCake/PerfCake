/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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
package org.perfcake.message.generator;

import org.perfcake.common.PeriodType;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.sender.MessageSenderManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class RampUpDownGenerator extends DefaultMessageGenerator {
   /**
    * The generator's logger.
    */
   private static final Logger log = Logger.getLogger(RampUpDownGenerator.class);

   /**
    * Phase of the generator.
    */
   private enum Phase {
      PRE, RAMP_UP, MAIN, RAMP_DOWN, POST;
   }

   /**
    * An initial number of threads.
    */
   private int preThreadCount = super.getThreads(); // default value taken from parent's threads

   /**
    * An final number of threads.
    */
   private int postThreadCount = super.getThreads(); // default value taken from parent's threads

   /**
    * A maximal number of threads.
    */
   private int mainThreadCount = super.getThreads();

   /**
    * A duration period after the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP} phase starts.
    */
   private long preDuration = Long.MAX_VALUE;

   /**
    * A number by which the # of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP} phase.
    */
   private int rampUpStep = 0;

   /**
    * A period after which the number of threads is changed by {@link org.perfcake.message.generator.RampUpDownGenerator#rampUpStep} value.
    */
   private long rampUpStepPeriod = Long.MAX_VALUE;

   /**
    * A number by which the number of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN} phase
    */
   private int rampDownStep = 0;

   /**
    * A period after which the number of threads is changed by {@link org.perfcake.message.generator.RampUpDownGenerator#rampDownStep} value.
    */
   private long rampDownStepPeriod = Long.MAX_VALUE;

   /**
    * A duration for which the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#MAIN} phase lasts.
    */
   private long mainDuration = 0;

   /**
    * A current phase of the generator.
    */
   private Phase currentPhase;

   @Override
   public void init(MessageSenderManager messageSenderManager, List<MessageTemplate> messageStore) throws Exception {
      super.init(messageSenderManager, messageStore);
      if (log.isInfoEnabled()) {
         log.info("Initiating " + getClass().getSimpleName());
      }
   }

   @Override
   public void generate() throws Exception {
      log.info("Starting to generate...");
      semaphore = new Semaphore(threadQueueSize);
      executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(preThreadCount);
      currentPhase = Phase.PRE;
      setThreads(preThreadCount);
      setStartTime();
      long last = 0;
      PeriodType runTimeType = runInfo.getDuration().getPeriodType();
      runInfo.addTag("");

      mainLoop:
      while (runInfo.isRunning()) {
         final long runTime;
         switch (runTimeType) {
            case TIME:
               runTime = runInfo.getRunTime();
               break;
            case ITERATION:
               runTime = runInfo.getIteration();
               break;
            default:
               // should not reach this code
               break mainLoop;
         }
         switch (currentPhase) {
            case PRE:
               if (runTime >= preDuration) {
                  currentPhase = Phase.RAMP_UP;
                  setThreads(getThreads() + rampUpStep);
                  resizeExecutorService(getThreads());
                  last = runTime;
               }
               break;
            case RAMP_UP:
               if (runTime - last >= rampUpStepPeriod) {
                  int newThreadCount = getThreads() + rampUpStep;
                  if (newThreadCount >= mainThreadCount) {
                     setThreads(mainThreadCount);
                     currentPhase = Phase.MAIN;
                  } else {
                     setThreads(newThreadCount);
                  }
                  last = runTime;
                  resizeExecutorService(getThreads());
               }
               break;
            case MAIN:
               if (runTime - last >= mainDuration) {
                  currentPhase = Phase.RAMP_DOWN;
                  setThreads(getThreads() - rampDownStep);
                  resizeExecutorService(getThreads());
                  last = runTime;
               }
               break;
            case RAMP_DOWN:
               if (runTime - last >= rampDownStepPeriod) {
                  int newThreadCount = getThreads() - rampDownStep;
                  if (newThreadCount < postThreadCount) {
                     if (newThreadCount <= 1) {
                        if (log.isEnabledFor(Level.WARN)) {
                           log.warn("There was an attempt to decrease the thread count below 1 in RAMP_DOWN phase. Setting number of threads to 1.");
                        }
                        newThreadCount = 1;
                     } else {
                        newThreadCount = postThreadCount;
                     }
                     currentPhase = Phase.POST;
                  }
                  last = runTime;
                  setThreads(newThreadCount);
                  resizeExecutorService(getThreads());
               }
               break;
            case POST:
            default:
               break;
         }

         logCurrentPhase(getThreads());
         super.prepareTask();
      }
      log.info("Reached test end. Shutting down execution...");
      super.shutdown();

   }

   private void logCurrentPhase(int threads) {
      if (log.isInfoEnabled()) {
         log.info(currentPhase.toString() + " phase [" + threads + " threads]");
      }
   }

   private void resizeExecutorService(int threads) throws InterruptedException {
      executorService.setCorePoolSize(threads);
      executorService.setMaximumPoolSize(threads);
   }

   public int getPreThreadCount() {
      return preThreadCount;
   }

   public RampUpDownGenerator setPreThreadCount(int preThreadCount) {
      this.preThreadCount = preThreadCount;
      return this;
   }

   public int getPostThreadCount() {
      return postThreadCount;
   }

   public RampUpDownGenerator setPostThreadCount(int postThreadCount) {
      this.postThreadCount = postThreadCount;
      return this;
   }

   public long getPreDuration() {
      return preDuration;
   }

   public RampUpDownGenerator setPreDuration(long preDuration) {
      this.preDuration = preDuration;
      return this;
   }

   public int getRampUpStep() {
      return rampUpStep;
   }

   public RampUpDownGenerator setRampUpStep(int rampUpStep) {
      this.rampUpStep = rampUpStep;
      return this;
   }

   public long getRampUpStepPeriod() {
      return rampUpStepPeriod;
   }

   public RampUpDownGenerator setRampUpStepPeriod(long rampUpStepPeriod) {
      this.rampUpStepPeriod = rampUpStepPeriod;
      return this;
   }

   public int getRampDownStep() {
      return rampDownStep;
   }

   public RampUpDownGenerator setRampDownStep(int rampDownStep) {
      this.rampDownStep = rampDownStep;
      return this;
   }

   public long getRampDownStepPeriod() {
      return rampDownStepPeriod;
   }

   public RampUpDownGenerator setRampDownStepPeriod(long rampDownStepPeriod) {
      this.rampDownStepPeriod = rampDownStepPeriod;
      return this;
   }

   public long getMainDuration() {
      return mainDuration;
   }

   public RampUpDownGenerator setMainDuration(long mainDuration) {
      this.mainDuration = mainDuration;
      return this;
   }

   public int getMainThreadCount() {
      return mainThreadCount;
   }

   public RampUpDownGenerator setMainThreadCount(int mainThreadCount) {
      this.mainThreadCount = mainThreadCount;
      return this;
   }
}
