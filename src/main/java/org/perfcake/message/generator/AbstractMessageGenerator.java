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
 * This represents the common ancestor for all generators. It can generate messages using a specified number ({@link #threads}) of concurrent messages senders (see {@link MessageSender}).
 * The generator should also have the ability to tag messages by the sequence number that indicated the order of messages.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
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
    * Represents the information about current run
    */
   protected RunInfo runInfo;

   /**
    * Initialize the generator. During the initialization the {@link #messageSenderManager} is initialized as well.
    *
    * @param messageSenderManager
    *           Message sender manager.
    * @param messageStore
    *           Message store where the messages are taken from.
    * @throws Exception
    */
   public void init(final MessageSenderManager messageSenderManager, final List<MessageTemplate> messageStore) throws Exception {
      this.messageStore = messageStore;
      this.messageSenderManager = messageSenderManager;
      this.messageSenderManager.init();
   }

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
    * Sets the message sender manager.
    *
    * @param messageSenderManager
    *           The message sender manager to set.
    */
   public void setMessageSenderManager(final MessageSenderManager messageSenderManager) {
      this.messageSenderManager = messageSenderManager;
   }

   /**
    * Sets the current run info
    *
    * @param runInfo
    *           The current run info object
    */
   public void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
      this.runInfo.setThreads(threads);
      validateRunInfo();
   }

   /**
    * Verifies that the current configuration of {@link org.perfcake.RunInfo} is supported by the current generator
    */
   abstract protected void validateRunInfo();

   /**
    * Sets the report manager
    *
    * @param reportManager
    *           The report manager to set.
    */
   public void setReportManager(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   /**
    * It closes and finalize the generator. During the closing the {@link #messageSenderManager} is closed as well.
    *
    * @throws PerfCakeException
    */
   public void close() throws PerfCakeException {
      messageSenderManager.close();
   }

   /**
    * Sets the timestamp of the moment when generator execution started.
    */
   protected void setStartTime() {
      reportManager.start();
   }

   /**
    * Sets the timestamp of the moment when generator execution stopped.
    */
   protected void setStopTime() {
      if (runInfo.isStarted()) {
         reportManager.stop();
      }
   }

   /**
    * Computes the current average speed the iterations are executed.
    *
    * @param cnt
    *           The iteration count.
    * @return The current average iteration execution speed.
    */
   protected float getSpeed(final long cnt) {
      return 1000f * cnt / runInfo.getRunTime();
   }

   /**
    * Executes the actual implementation of a generator.
    *
    * @throws Exception
    */
   public abstract void generate() throws Exception;

   /**
    * Executes the steps needed after the moment the warm-up period ended.
    *
    * @throws Exception
    */
   protected void postWarmUp() throws Exception {
      // override if needed
   }

   /**
    * Used to read the value of threads.
    *
    * @return the threads
    */
   public int getThreads() {
      return threads;
   }

   /**
    * Sets the number of threads.
    *
    * @param threads
    *           The number of threads.
    * @return this
    */
   public AbstractMessageGenerator setThreads(final int threads) {
      this.threads = threads;
      if (runInfo != null) {
         runInfo.setThreads(threads);
      }

      return this;
   }

   /**
    * Used to read the value of messageNumberingEnabled.
    *
    * @return The messageNumberingEnabled value.
    */
   public boolean isMessageNumberingEnabled() {
      return messageNumberingEnabled;
   }

   /**
    * Sets the value of messageNumberingEnabled.
    *
    * @param messageNumberingEnabled
    *           The messageNumberingEnabled to set.
    * @return this
    */
   public AbstractMessageGenerator setMessageNumberingEnabled(final boolean messageNumberingEnabled) {
      this.messageNumberingEnabled = messageNumberingEnabled;
      return this;
   }

   public void setValidationManager(final ValidationManager validationManager) {
      this.validationManager = validationManager;
   }
}
