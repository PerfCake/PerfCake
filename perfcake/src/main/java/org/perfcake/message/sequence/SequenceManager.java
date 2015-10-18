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
package org.perfcake.message.sequence;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Keeps a registry of existing sequences.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SequenceManager {

   private static final Logger log = LogManager.getLogger(SequenceManager.class);

   /**
    * Registry of sequences.
    */
   private Map<String, Sequence> sequences = new HashMap<>();

   /**
    * Gets a default {@link SequenceManager} instance with a default number sequence prepared for message numbering (store under key {@link PerfCakeConst#MESSAGE_NUMBER_PROPERTY}).
    */
   public SequenceManager() {
      try {
         addSequence(PerfCakeConst.MESSAGE_NUMBER_PROPERTY, new NumberSequence());
         addSequence(PerfCakeConst.CURRENT_TIMESTAMP_PROPERTY, new TimeStampSequence());
         addSequence(PerfCakeConst.THREAD_ID_PROPERTY, new ThreadIdSequence());
      } catch (PerfCakeException e) {
         log.warn("Cannot initialize default sequences: ", e);
      }
   }

   /**
    * Registers a new sequence in the registry.
    *
    * @param name
    *       Sequence name.
    * @param sequence
    *       Sequence instance.
    * @throws PerfCakeException
    *       When it was not possible to properly initialize the newly added sequence.
    */
   public void addSequence(final String name, final Sequence sequence) throws PerfCakeException {
      sequences.put(name, sequence);
      sequence.reset();
   }

   /**
    * Gets a snapshot of current next values of all sequences in the registry using {@link Sequence#getNext()}.
    *
    * @return Snapshot of the values as properties in the form sequence name -&gt; sequence next value.
    */
   public synchronized Properties getSnapshot() {
      final Properties snapshot = new Properties();

      sequences.forEach((name, sequence) -> snapshot.setProperty(name, sequence.getNext()));

      return snapshot;
   }
}
