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

import org.perfcake.message.correlator.Correlator;

/**
 * Represents a thread for receiving messages from a separate message channel.
 * All received messages are passed to the correlator which notifies the correct {@link org.perfcake.message.generator.SenderTask}.
 * Receivers are started as threads and stopped with {@link Thread#interrupt()}. It is up to the receiver to react accordingly.
 * Receivers are executed as daemon threads and can be terminated at the end of the test execution if they do not react to the interruption.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Receiver extends Runnable {

   void setCorrelator(final Correlator correlator);

}
