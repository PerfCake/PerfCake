package org.perfcake.message.generator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.common.PeriodType;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.sender.MessageSenderManager;

import java.util.List;
import java.util.concurrent.Executors;
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
   private int preThreadCount = super.threads; // default value taken from parent's threads

   /**
    * An final number of threads.
    */
   private int postThreadCount = super.threads; // default value taken from parent's threads

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
      executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(preThreadCount);
      currentPhase = Phase.PRE;
      int currentThreadCount = preThreadCount;
      setStartTime();
      long last = 0;
      PeriodType runTimeType = runInfo.getDuration().getPeriodType();
      runInfo.addTag("");

      mainLoop: while (runInfo.isRunning()) {
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
                  currentThreadCount = currentThreadCount + rampUpStep;
                  resizeExecutorService(currentThreadCount);
                  last = runTime;
               }
               break;
            case RAMP_UP:
               if (runTime - last >= rampUpStepPeriod) {
                  currentThreadCount = currentThreadCount + rampUpStep;
                  if (currentThreadCount > threads) {
                     currentThreadCount = threads;
                     currentPhase = Phase.MAIN;
                  }
                  last = runTime;
                  resizeExecutorService(currentThreadCount);
               }
               break;
            case MAIN:
               if (runTime - last >= mainDuration) {
                  currentPhase = Phase.RAMP_DOWN;
                  currentThreadCount = currentThreadCount - rampDownStep;
                  resizeExecutorService(currentThreadCount);
                  last = runTime;
               }
               break;
            case RAMP_DOWN:
               if (runTime - last >= rampDownStepPeriod) {
                  currentThreadCount = currentThreadCount - rampDownStep;
                  if (currentThreadCount < postThreadCount) {
                     if (currentThreadCount <= 1) {
                        if (log.isEnabledFor(Level.WARN)) {
                           log.warn("There was an attempt to decrease the thread count below 1 in RAMP_DOWN phase. Setting number of threads to 1.");
                        }
                        currentThreadCount = 1;
                     } else {
                        currentThreadCount = postThreadCount;
                     }
                     currentPhase = Phase.POST;
                  }
                  last = runTime;
                  resizeExecutorService(currentThreadCount);
               }
               break;
            case POST:
            default:
               break;
         }

         logCurrentPhase(currentThreadCount);
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

   public void setPreThreadCount(int preThreadCount) {
      this.preThreadCount = preThreadCount;
   }

   public int getPostThreadCount() {
      return postThreadCount;
   }

   public void setPostThreadCount(int postThreadCount) {
      this.postThreadCount = postThreadCount;
   }

   public long getPreDuration() {
      return preDuration;
   }

   public void setPreDuration(long preDuration) {
      this.preDuration = preDuration;
   }

   public int getRampUpStep() {
      return rampUpStep;
   }

   public void setRampUpStep(int rampUpStep) {
      this.rampUpStep = rampUpStep;
   }

   public long getRampUpStepPeriod() {
      return rampUpStepPeriod;
   }

   public void setRampUpStepPeriod(long rampUpStepPeriod) {
      this.rampUpStepPeriod = rampUpStepPeriod;
   }

   public int getRampDownStep() {
      return rampDownStep;
   }

   public void setRampDownStep(int rampDownStep) {
      this.rampDownStep = rampDownStep;
   }

   public long getRampDownStepPeriod() {
      return rampDownStepPeriod;
   }

   public void setRampDownStepPeriod(long rampDownStepPeriod) {
      this.rampDownStepPeriod = rampDownStepPeriod;
   }

   public long getMainDuration() {
      return mainDuration;
   }

   public void setMainDuration(long mainDuration) {
      this.mainDuration = mainDuration;
   }
}
