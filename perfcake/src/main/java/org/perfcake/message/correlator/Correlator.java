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
package org.perfcake.message.correlator;

import org.perfcake.message.Message;
import org.perfcake.message.generator.SenderTask;

import java.io.Serializable;
import java.util.Properties;

import io.vertx.core.MultiMap;

/**
 * Correlates requests with their responses and notifies {@link SenderTask} of receiving the appropriate response to the
 * original request. This is done based on a correlation id that is extracted from both request and response.
 * Upon a successful match, {@link SenderTask#registerResponse(Serializable)} is called.
 * For performance reasons, all interface methods should be implemented thread safe without locking and or synchronization.
 * All implementations should make sure that they do not keep eating up the memory and clean their data structures regularly.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Correlator {

   /**
    * Registers a request to be sent.
    *
    * @param senderTask
    *       {@link SenderTask} controlling the sending of the message.
    * @param message
    *       The message to be sent and from which the correlation id is extracted.
    * @param messageAttributes
    *       Message attributes of the message to be sent. Can help with correlation id extraction.
    */
   void registerRequest(final SenderTask senderTask, final Message message, final Properties messageAttributes);

   /**
    * Extracts the correlation id from the response and calls the corresponding sender task's {@link SenderTask#registerResponse(Serializable)} method.
    * Called from a receiver upon receiving a response.
    *
    * @param response
    *       The response received by {@link org.perfcake.message.receiver.Receiver}.
    * @param headers
    *       Headers received with the response. Can be useful for discovering correlation id.
    */
   void registerResponse(final Serializable response, final MultiMap headers);
}
