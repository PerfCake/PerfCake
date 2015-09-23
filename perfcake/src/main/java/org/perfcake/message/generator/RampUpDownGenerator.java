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

import org.perfcake.PerfCakeException;
import org.perfcake.common.PeriodType;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.sender.MessageSenderManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Generates maximal load using a variable number of threads.
 * <p>The generating starts with the number of threads set to the value of the {@link #preThreadCount} property.
 * It continues to execute for the duration set by the {@link #preDuration} property.
 * The period is called the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE PRE} phase.
 * When {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE PRE} phase ends,
 * the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase starts.</p>
 *
 * <p>In the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase
 * the number of threads is changed by the value of the {@link #rampUpStep} property
 * each period set by the {@link #rampUpStepPeriod} until it reaches the number of threads
 * set by the value of the {@link #mainThreadCount} property.</p>
 *
 * <p>In that moment {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#MAIN MAIN} phase starts
 * and the execution continues for the duration set by the {@link #mainDuration} property,
 * after which the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN RAMP DOWN} phase starts.</p>
 *
 * <p>In the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN RAMP DOWN} phase
 * the number of threads is again changed but this time in the opposite direction than
 * in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase.
 * It changes by the value of the {@link #rampDownStep} property each period specified
 * by the {@link #rampDownStepPeriod} property until the final number of threads is reached.
 * By that moment the final phase called {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#POST POST} starts.</p>
 *
 * <p>The {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#POST POST} phase ends by the end of the scenario execution.</p>
 *
 * <p>The outer borders of the number of threads and the duration is set by the maximum number of threads
 * specified by the <code>threads</code> attribute of the generator and by the maximum duration set by the <code>run</code> element.</p>
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class RampUpDownGenerator extends DefaultMessageGenerator {
   /**
    * The generator's logger.
    */
   private static final Logger log = LogManager.getLogger(RampUpDownGenerator.class);

   /**
    * Phase of the generator.
    */
   private enum Phase {
      PRE, RAMP_UP, MAIN, RAMP_DOWN, POST
   }

   /**
    * An initial number of threads.
    */
   private int preThreadCount = -1;

   /**
    * A final number of threads.
    */
   private int postThreadCount = -1;

   /**
    * A maximal number of threads.
    */
   private int mainThreadCount = -1;

   /**
    * A duration period of the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE} phase,
    * before the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP} phase starts.
    */
   private long preDuration = Long.MAX_VALUE;

   /**
    * A number by which the number of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP} phase.
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
   public void init(final MessageSenderManager messageSenderManager, final List<MessageTemplate> messageStore) throws PerfCakeException {
      super.init(messageSenderManager, messageStore);
      if (preThreadCount <= 0) {
         preThreadCount = super.getThreads();
      }
      if (mainThreadCount <= 0) {
         mainThreadCount = super.getThreads();
      }
      if (postThreadCount <= 0) {
         postThreadCount = super.getThreads();
      }
   }

   @Override
   public void generate() throws Exception {
      log.info("Starting to generate...");
      semaphore = new Semaphore(senderTaskQueueSize);
      executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(preThreadCount);
      currentPhase = Phase.PRE;
      boolean phaseChanged = true;
      setThreads(preThreadCount);
      boolean threadCountChanged = true;
      setStartTime();
      long last = 0;
      final PeriodType runTimeType = runInfo.getDuration().getPeriodType();
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
                  phaseChanged = true;
                  setThreads(getThreads() + rampUpStep);
                  resizeExecutorService(getThreads());
                  last = runTime;
               }
               break;
            case RAMP_UP:
               if (runTime - last >= rampUpStepPeriod) {
                  final int newThreadCount = getThreads() + rampUpStep;
                  if (newThreadCount >= mainThreadCount) {
                     setThreads(mainThreadCount);
                     currentPhase = Phase.MAIN;
                     phaseChanged = true;
                  } else {
                     setThreads(newThreadCount);
                  }
                  last = runTime;
                  resizeExecutorService(getThreads());
                  threadCountChanged = true;
               }
               break;
            case MAIN:
               if (runTime - last >= mainDuration) {
                  currentPhase = Phase.RAMP_DOWN;
                  phaseChanged = true;
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
                        if (log.isWarnEnabled()) {
                           log.warn("There was an attempt to decrease the thread count below 1 in RAMP_DOWN phase. Setting number of threads to 1.");
                        }
                        newThreadCount = 1;
                     } else {
                        newThreadCount = postThreadCount;
                     }
                     currentPhase = Phase.POST;
                     phaseChanged = true;
                  }
                  last = runTime;
                  setThreads(newThreadCount);
                  resizeExecutorService(getThreads());
                  threadCountChanged = true;
               }
               break;
            case POST:
            default:
               break;
         }
         if (phaseChanged || threadCountChanged) {
            if (log.isDebugEnabled()) {
               log.debug(currentPhase.toString() + " phase [" + getThreads() + " threads]");
            }
            phaseChanged = false;
            threadCountChanged = false;
         }
         super.prepareTask();
      }
      log.info("Reached test end. Shutting down execution...");
      super.shutdown();

   }

   private void resizeExecutorService(final int threads) throws InterruptedException {
      executorService.setCorePoolSize(threads);
      executorService.setMaximumPoolSize(threads);
   }

   /**
    * Gets an initial number of threads - the number of threads in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE PRE} phase.
    *
    * @return The final number of threads.
    */
   public int getPreThreadCount() {
      return preThreadCount;
   }

   /**
    * Sets a final number of threads - the number of threads in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE PRE} phase.
    *
    * @param preThreadCount
    *       The initial number of threads.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setPreThreadCount(final int preThreadCount) {
      this.preThreadCount = preThreadCount;
      return this;
   }

   /**
    * Gets a final number of threads - the number of threads in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#POST POST} phase.
    *
    * @return The final number of threads.
    */
   public int getPostThreadCount() {
      return postThreadCount;
   }

   /**
    * Sets a final number of threads - the number of threads in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#POST POST} phase.
    *
    * @param postThreadCount
    *       The final number of threads.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setPostThreadCount(final int postThreadCount) {
      this.postThreadCount = postThreadCount;
      return this;
   }

   /**
    * Gets duration period of the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE PRE} phase,
    * before the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase starts.
    *
    * @return PRE phase duration period in the units of <code>run</code> type.
    */
   public long getPreDuration() {
      return preDuration;
   }

   /**
    * Sets duration period of the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#PRE PRE} phase,
    * before the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase starts.
    *
    * @param preDuration
    *       PRE phase duration period in the units of <code>run</code> type.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setPreDuration(final long preDuration) {
      this.preDuration = preDuration;
      return this;
   }

   /**
    * Gets a number by which the number of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase.
    *
    * @return The size of the step.
    */
   public int getRampUpStep() {
      return rampUpStep;
   }

   /**
    * Sets a number by which the number of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase.
    *
    * @param rampUpStep
    *       The size of the step.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setRampUpStep(final int rampUpStep) {
      this.rampUpStep = rampUpStep;
      return this;
   }

   /**
    * Gets a duration period after which the number of threads is changed in {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase.
    *
    * @return The RAMP UP step duration period in a units of <code>run</code> type.
    */
   public long getRampUpStepPeriod() {
      return rampUpStepPeriod;
   }

   /**
    * Sets a duration period after which the number of threads is changed in {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_UP RAMP UP} phase.
    *
    * @param rampUpStepPeriod
    *       The RAMP UP step duration period in a units of <code>run</code> type.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setRampUpStepPeriod(final long rampUpStepPeriod) {
      this.rampUpStepPeriod = rampUpStepPeriod;
      return this;
   }

   /**
    * Gets a number by which the number of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN RAMP DOWN} phase.
    *
    * @return The size of the step.
    */
   public int getRampDownStep() {
      return rampDownStep;
   }

   /**
    * Sets a number by which the number of threads is changed in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN RAMP DOWN} phase.
    *
    * @param rampDownStep
    *       The size of the step.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setRampDownStep(final int rampDownStep) {
      this.rampDownStep = rampDownStep;
      return this;
   }

   /**
    * Gets a duration period after which the number of threads is changed in {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN RAMP DOWN} phase.
    *
    * @return The RAMP DOWN step duration period in a units of <code>run</code> type.
    */
   public long getRampDownStepPeriod() {
      return rampDownStepPeriod;
   }

   /**
    * Sets a duration period after which the number of threads is changed in {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#RAMP_DOWN RAMP DOWN} phase.
    *
    * @param rampDownStepPeriod
    *       The RAMP DOWN step duration period in a units of <code>run</code> type.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setRampDownStepPeriod(final long rampDownStepPeriod) {
      this.rampDownStepPeriod = rampDownStepPeriod;
      return this;
   }

   /**
    * Gets a duration period for which the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#MAIN MAIN} phase lasts.
    *
    * @return The MAIN phase duration period in the units of <code>run</code> type.
    */
   public long getMainDuration() {
      return mainDuration;
   }

   /**
    * Sets a duration period for which the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#MAIN MAIN} phase lasts.
    *
    * @param mainDuration
    *       The MAIN phase duration period in the units of <code>run</code> type.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setMainDuration(final long mainDuration) {
      this.mainDuration = mainDuration;
      return this;
   }

   /**
    * Gets a maximal number of threads - the number of threads used in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#MAIN MAIN} phase.
    *
    * @return The maximal number of threads.
    */
   public int getMainThreadCount() {
      return mainThreadCount;
   }

   /**
    * Sets a maximal number of threads - the number of threads used in the {@link org.perfcake.message.generator.RampUpDownGenerator.Phase#MAIN MAIN} phase.
    *
    * @param mainThreadCount
    *       The maximal number of threads.
    * @return Instance of this to support fluent API.
    */
   public RampUpDownGenerator setMainThreadCount(final int mainThreadCount) {
      this.mainThreadCount = mainThreadCount;
      return this;
   }
}
