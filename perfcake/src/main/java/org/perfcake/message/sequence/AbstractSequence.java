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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class AbstractSequence implements Sequence {

   private CompletableFuture<String> nextValue = null;

   @Override
   public final String getNext() {
      final CompletableFuture<String> previousValue = nextValue;
      nextValue = CompletableFuture.supplyAsync(this::doGetNext);
      try {
         return previousValue.get();
      } catch (InterruptedException | ConcurrentModificationException | ExecutionException e) {
         return doGetNext(); // fallback to the original sequence
      }
   }

   @Override
   public final void reset() throws PerfCakeException {
      doReset();
      nextValue = CompletableFuture.supplyAsync(this::doGetNext);
   }

   abstract public String doGetNext();

   abstract public void doReset() throws PerfCakeException;
}
