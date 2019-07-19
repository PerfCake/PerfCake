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
import java.util.UUID;

/**
 * Sequence of random UUIDs.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RandomUuidSequence implements Sequence {

   @Override
   public final void publishNext(final String sequenceId, final Properties values) {
      values.setProperty(sequenceId, UUID.randomUUID().toString());
   }

   @Override
   public void reset() throws PerfCakeException {
      // we could create a new instance of random, but it does not make much sense
   }
}
