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
package org.perfcake.message.receiver;

import org.perfcake.PerfCakeException;
import org.perfcake.message.correlator.Correlator;

import java.util.Properties;

/**
 * Represents a receiver for receiving messages from a separate message channel.
 * All received messages are passed to the correlator which notifies the correct {@link org.perfcake.message.generator.SenderTask}.
 * A receiver must start the defined (using the {@link Receiver#setThreads(int)} method) threads to receive messages.
 * These threads are later stopped with {@link Thread#interrupt()}. It is up to the receiver threads to react accordingly.
 * Receivers must be executed as daemon threads and can be terminated at the end of the test execution if they do not react to the interruption.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Receiver {

   /**
    * Sets the number of threads that will be receiving responses.
    * @param threadCount The number of threads that will be receiving responses.
    */
   void setThreads(final int threadCount);

   /**
    * Sets the correlator that will be handling the responses.
    * @param correlator The correlator that will be handling the responses.
    */
   void setCorrelator(final Correlator correlator);

   /**
    * Starts all receiver threads.
    * @throws PerfCakeException When it was not possible to start all threads.
    */
   void start() throws PerfCakeException;

   /**
    * Stops all the receiver threads.
    */
   void stop();

   /**
    * Gets the source from where to receive the messages.
    *
    * @return The current source (typically an URI).
    */
   String getSource();

   /**
    * Sets the source from where to receive the messages.
    *
    * @param source
    *       The source to be set.
    * @return Instance of this for fluent API.
    */
   Receiver setSource(final String source);
}
