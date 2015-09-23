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
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequence.SequenceManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidationManager;

import java.util.List;

/**
 * A definition of contract for all message generators.
 *
 * A message generator controls how many threads are being used to generate the messages, is responsible for creating and submitting
 * {@link org.perfcake.message.generator.SenderTask SenderTasks} and controls the other components involved in the performance test execution.
 *
 * A message generator is the most crucial and complicated component of PerfCake and it is highly recommended to reuse one of existing
 * implementations as they already offer mostly wanted features.
 *
 * The main task of a message generator is to take care of the sending threads, create {@link SenderTask}s as needed and monitor test progress.
 * It is important to properly shutdown the message generation for both time and iteration based test length control. In the case of an iteration based
 * control, a generator must wait for all the messages to be processed. In the case of a time based control, the test stops immediately after the time
 * has elapsed.
 *
 * Each {@link SenderTask} takes a {@link java.util.concurrent.Semaphore} that is released once all its work is finished. This can be used by a generator
 * to control the number of actually active {@link SenderTask}s.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface MessageGenerator {

   /**
    * Initializes the generator. During the initialization the {@link MessageSenderManager} should be initialized as well.
    *
    * @param messageSenderManager
    *       Message sender manager.
    * @param messageStore
    *       Message store where the messages are taken from.
    * @throws PerfCakeException
    *       When it was not possible to initialize the generator.
    */
   void init(final MessageSenderManager messageSenderManager, final List<MessageTemplate> messageStore) throws PerfCakeException;

   /**
    * Generates the messages. This actually executes the whole performance test.
    *
    * @throws Exception
    *       When it was not possible to generate the messages.
    */
   void generate() throws Exception;

   /**
    * Interrupts the execution with a message from failed sender.
    *
    * @param exception
    *       The cause of the interruption.
    */
   void interrupt(final Exception exception);

   /**
    * Closes and finalizes the generator. The {@link MessageSenderManager} must be closed as well.
    *
    * @throws PerfCakeException
    *       When it was not possible to smoothly finalize the generator.
    */
   void close() throws PerfCakeException;

   /**
    * Sets the current {@link org.perfcake.RunInfo} to control generating of the messages.
    *
    * @param runInfo
    *       {@link org.perfcake.RunInfo} to be used.
    */
   void setRunInfo(final RunInfo runInfo);

   /**
    * Sets the {@link org.perfcake.reporting.ReportManager} to be used for the current performance test execution.
    *
    * @param reportManager
    *       {@link org.perfcake.reporting.ReportManager} to be used.
    */
   void setReportManager(final ReportManager reportManager);

   /**
    * Configures the {@link org.perfcake.validation.ValidationManager} to be used for the performance test execution.
    *
    * @param validationManager
    *       {@link org.perfcake.validation.ValidationManager} to be used
    */
   void setValidationManager(final ValidationManager validationManager);

   /**
    * Sets a manager of sequences that can be used to replace placeholders in a message template and sender's target.
    *
    * @param sequenceManager
    *       The {@link SequenceManager} to be used to replace placeholders in a message template and sender's target.
    */
   void setSequenceManager(final SequenceManager sequenceManager);

   /**
    * Gets the number of threads that should be used to generate the messages.
    * The return value can change over time during the test execution.
    *
    * @return Number of currently running threads.
    */
   int getThreads();

   /**
    * Sets the number of threads used to generate the messages. This can be changed during a running test.
    *
    * @param threads
    *       The number of threads to be used.
    * @return Instance of this to support fluent API.
    */
   MessageGenerator setThreads(final int threads);

   /**
    * Gets the number of active threads used currently by the generator.
    * This can help determine if there were some threads blocked waiting for reply.
    *
    * @return Number of active threads in use.
    */
   int getActiveThreadsCount();
}
