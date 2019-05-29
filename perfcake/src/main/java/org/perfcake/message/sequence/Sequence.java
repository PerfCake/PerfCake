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
package org.perfcake.message.sequence;

import org.perfcake.PerfCakeException;

import java.util.Properties;

/**
 * Represents an automatically generated sequence of values.
 * The resulting values can be used in the message body.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Sequence {

   /**
    * Asks the sequence to publish its next value(s) to the provided map. The map can be considered thread safe.
    *
    * @param sequenceId
    *       Id of the sequence instance.
    * @param values
    *       The properties to provide the next value(s) to.
    */
   void publishNext(final String sequenceId, final Properties values);

   /**
    * Resets the sequence.
    * This method is called at the very beginning, so it can be used to perform any initialization steps as well.
    *
    * @throws PerfCakeException
    *       When it was not possible to initialize the sequence to its original state.
    */
   default void reset() throws PerfCakeException {
   }
}
