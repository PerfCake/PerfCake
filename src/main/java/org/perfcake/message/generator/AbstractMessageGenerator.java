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
import org.perfcake.RunInfo;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidationManager;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A common ancestor for all generators. It can generate messages in parallel using {@link MessageSender Message Senders} running
 * concurrently in {@link #threads} number of threads.
 * The generator should also have the ability to tag messages by the sequence number that indicated the order of messages
 * (see {@link #setMessageNumberingEnabled(boolean)}).
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractMessageGenerator {

   /**
    * Message sender manager.
    */
   protected MessageSenderManager messageSenderManager;

   /**
    * Report manager.
    */
   protected ReportManager reportManager;

   /**
    * A reference to the current message validator manager.
    */
   protected ValidationManager validationManager;

   /**
    * Message store where the messages for senders to be send are taken from.
    */
   protected List<MessageTemplate> messageStore;

   /**
    * Number of concurrent threads the generator will use to send the messages.
    */
   private int threads = 1;

   /**
    * The executor service used to run the threads.
    */
   protected ThreadPoolExecutor executorService;

   /**
    * The property of the generator indicating whether the message numbering feature is enabled or disabled.
    */
   protected boolean messageNumberingEnabled = false;

   /**
    * Represents the information about current run.
    */
   protected RunInfo runInfo;

   /**
    * Initialize the generator. During the initialization the {@link #messageSenderManager} is initialized as well.
    *
    * @param messageSenderManager
    *       Message sender manager.
    * @param messageStore
    *       Message store where the messages are taken from.
    * @throws Exception
    *       When it was not possible to initialize the generator.
    */
   public void init(final MessageSenderManager messageSenderManager, final List<MessageTemplate> messageStore) throws Exception {
      this.messageStore = messageStore;
      this.messageSenderManager = messageSenderManager;
      this.messageSenderManager.init();
   }

   /**
    * Gets a new instance of a {@link org.perfcake.message.generator.SenderTask}.
    * The provided semaphore can be used to control parallel execution of sender tasks in multiple threads.
    *
    * @param semaphore Semaphore that will be release upon completion of the sender task.
    *
    * @return A sender task ready to work on another iteration.
    */
   protected SenderTask newSenderTask(Semaphore semaphore) {
      final SenderTask task = new SenderTask(semaphore);

      task.setMessageStore(messageStore);
      task.setReportManager(reportManager);
      task.setSenderManager(messageSenderManager);
      task.setValidationManager(validationManager);
      task.setMessageNumberingEnabled(isMessageNumberingEnabled());

      return task;
   }

   /**
    * Sets the {@link org.perfcake.message.sender.MessageSenderManager} to be used for generating the messages.
    *
    * @param messageSenderManager
    *       The message sender manager to set.
    */
   public void setMessageSenderManager(final MessageSenderManager messageSenderManager) {
      this.messageSenderManager = messageSenderManager;
   }

   /**
    * Sets the current {@link org.perfcake.RunInfo} to control generating of the messages.
    *
    * @param runInfo {@link org.perfcake.RunInfo} to be used.
    */
   public void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
      this.runInfo.setThreads(threads);
      validateRunInfo();
   }

   /**
    * Verifies that the current configuration of {@link org.perfcake.RunInfo} is supported by the current generator.
    */
   abstract protected void validateRunInfo();

   /**
    * Sets the {@link org.perfcake.reporting.ReportManager} to be used for the current performance test execution.
    *
    * @param reportManager {@link org.perfcake.reporting.ReportManager} to be used.
    */
   public void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   /**
    * Closes and finalizes the generator. The {@link #messageSenderManager} is closed as well.
    *
    * @throws PerfCakeException
    *       When it was not possible to smoothly finalize the generator.
    */
   public void close() throws PerfCakeException {
      messageSenderManager.close();
   }

   /**
    * Records the timestamp of the moment when the generator execution started.
    */
   protected void setStartTime() {
      reportManager.start();
   }

   /**
    * Records the timestamp of the moment when the generator execution stopped.
    */
   protected void setStopTime() {
      if (runInfo.isStarted()) {
         reportManager.stop();
      }
   }

   /**
    * Computes the current average speed at which the iterations are executed.
    *
    * @param cnt Current iteration number.
    *
    * @return The current average iteration execution speed in iterations per second.
    */
   protected float getSpeed(final long cnt) {
      return 1000f * cnt / runInfo.getRunTime();
   }

   /**
    * Generate the messages. This actually executes the whole performance test.
    *
    * @throws Exception
    *       When it was not possible to generate the messages.
    */
   public abstract void generate() throws Exception;

   /**
    * Gets the number of threads that should be used to generate the messages.
    * The return value can change over time during the test execution.
    *
    * @return Number of currently running threads.
    */
   public int getThreads() {
      return threads;
   }

   /**
    * Sets the number of threads used to generate the messages.
    *
    * @param threads The number of threads to be used.
    *
    * @return Returns this instance for fluent API.
    */
   public AbstractMessageGenerator setThreads(final int threads) {
      this.threads = threads;
      if (runInfo != null) {
         runInfo.setThreads(threads);
      }

      return this;
   }

   /**
    * Is the message numbering enabled? When enabled, each message gets a unique number assigned. This should be disabled
    * for maximal performance.
    *
    * @return True if and only if the numbering is enabled.
    */
   public boolean isMessageNumberingEnabled() {
      return messageNumberingEnabled;
   }

   /**
    * Enables or disables marking the messages with a unique number. Disable this for maximal performance.
    *
    * @param messageNumberingEnabled
    *       True to enable message numbering, false otherwise.
    *
    * @return Returns this instance for fluent API.
    */
   public AbstractMessageGenerator setMessageNumberingEnabled(final boolean messageNumberingEnabled) {
      this.messageNumberingEnabled = messageNumberingEnabled;
      return this;
   }

   /**
   * Configures the {@link org.perfcake.validation.ValidationManager} to be used for the performance test execution.
   *
   * @param validationManager {@link org.perfcake.validation.ValidationManager} to be used.s
   */
   public void setValidationManager(final ValidationManager validationManager) {
      this.validationManager = validationManager;
   }
}
