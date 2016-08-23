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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps a registry of existing sequences.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SequenceManager {

   /**
    * Registry of sequences.
    */
   private Map<String, Sequence> sequences = new HashMap<>();

   /**
    * Registers a new sequence in the registry.
    *
    * @param id
    *       Sequence id.
    * @param sequence
    *       Sequence instance.
    * @throws PerfCakeException
    *       When it was not possible to properly initialize the newly added sequence.
    */
   public void addSequence(final String id, final Sequence sequence) throws PerfCakeException {
      sequences.put(id, sequence);
      sequence.reset();
   }

   /**
    * Gets a snapshot of current next values of all sequences in the registry using {@link Sequence#publishNext(String, Properties)}.
    *
    * @return Snapshot of the values as properties in the form sequence name -&gt; sequence next value.
    */
   public Properties getSnapshot() {
      final Properties snapshot = new Properties();

      sequences.forEach((k, v) -> v.publishNext(k, snapshot));

      return snapshot;
   }

}
