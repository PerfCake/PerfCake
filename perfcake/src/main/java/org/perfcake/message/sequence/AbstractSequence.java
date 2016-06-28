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

import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This abstract sequence helps with pre-calculating the next value in the row in a background thread
 * if the computation of the value is complex. None of the built-in sequences actually do not use this class.
 * However, any extension might benefit from it.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class AbstractSequence implements Sequence {

   private AtomicReference<CompletableFuture<String>> nextValue = new AtomicReference<>();

   @Override
   @SuppressWarnings("unchecked")
   public final String getNext() {
      // cannot use get and set as this could break the order of values (the task added later can be computed sooner)
      final CompletableFuture<String> previousValue = nextValue.get();
      try {
         final String result = previousValue.get();
         nextValue.set(CompletableFuture.supplyAsync(this::doGetNext));

         return result;
      } catch (InterruptedException | ConcurrentModificationException | ExecutionException e) {
         return doGetNext(); // fallback to the original sequence
      }
   }

   @Override
   @SuppressWarnings("unchecked")
   public final void reset() throws PerfCakeException {
      doReset();
      nextValue.set(CompletableFuture.supplyAsync(this::doGetNext));
   }

   public abstract String doGetNext();

   public abstract void doReset() throws PerfCakeException;
}
