/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2014 - 2016 the original author or authors.
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
package org.perfcake.common;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 * A structure to keep record of elements for a certain period of time. It supports concurrent access and
 * has amortized time complexity to add and remove element of O(1) (each removal is prepaid while the element is added).
 *
 * The window keeps time order of the elements and although it offers the possibility of providing artificial time, it still
 * needs that the objects are added in a correct time order (older objects first).
 *
 * The time window is closed on both ends. For example, a sliding window all length 500 at the current time 600 will have both objects
 * for time 100 and 600.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TimeSlidingWindow<E> {

   /**
    * The internal data structure maintaining the records of objects together with the time when they were added.
    */
   private final Deque<TemporalObject<E>> dequeue = new ConcurrentLinkedDeque<>();

   /**
    * The length of the window in milliseconds.
    */
   private final long length;

   /**
    * Creates a new time sliding window of the given length in milliseconds.
    *
    * @param windowLength
    *       The length of the sliding window in milliseconds.
    */
   public TimeSlidingWindow(final long windowLength) {
      length = windowLength;
   }

   /**
    * Gets the length of the sliding window in milliseconds.
    *
    * @return The length of the sliding window in milliseconds.
    */
   public long getLength() {
      return length;
   }

   /**
    * Adds an object to the sliding window. It stays in the window for the time specified by the sliding window length.
    *
    * @param object
    *       Object to be added.
    */
   public void add(final E object) {
      dequeue.add(new TemporalObject<E>(object));
   }

   /**
    * Adds an object to the sliding without with an information about artificial time. The object cannot be older than the one added previously
    * (the structure needs to keep time order of elements).
    *
    * @param object
    *       The object to be added.
    * @param time
    *       Artificial time when the object was added to the window.
    */
   public void add(final E object, final long time) {
      if (dequeue.peekLast() == null || dequeue.peekLast().time <= time) {
         dequeue.add(new TemporalObject<>(object, time));
      } else {
         throw new IllegalStateException("Cannot enqueue an element older then the last added.");
      }
   }

   /**
    * Performs the given action for each element of the sliding window
    * until all elements have been processed or the action throws an
    * exception.
    *
    * Only the elements in the valid time window are processed. All older elements are garbage collected.
    *
    * @param action
    *       The action to be performed for each element.
    */
   public void forEach(final Consumer<E> action) {
      gc();

      dequeue.forEach(te -> action.accept(te.object));
   }

   /**
    * Performs the given action for each element of the sliding window until all elements have been processed or the action throws an
    * exception.
    *
    * Only the elements in the valid time window are processed. All older elements are garbage collected.
    *
    * This action might not return all elements when 1) it was already called with a higher (later) current time, 2) {@link #forEach(Consumer)}
    * was already called which actually propagated a higher (later) time to this method.
    *
    * @param currentTime
    *       Artificial time to control the position of the sliding window.
    * @param action
    *       The action to be performed for each element.
    */
   public void forEach(final long currentTime, final Consumer<E> action) {
      gc(currentTime);

      for (final TemporalObject<E> te: dequeue) {
         if (te.time <= currentTime) {
            action.accept(te.object);
         } else {
            break;
         }
      }
   }

   /**
    * Performs a garbage collection of elements that are out of the time window.
    * It is not necessary to call this method manually, however, it might be useful to cut
    * the memory costs when {@link #forEach} was not called for a long time.
    */
   public void gc() {
      gc(System.currentTimeMillis());
   }

   /**
    * Performs a garbage collection based on the provided time information.
    * This might be useful when controlling the time artificially.
    *
    * @param time
    *       Current artificial time.
    */
   public void gc(final long time) {
      // remove all leading old objects
      TemporalObject<E> e = dequeue.peek();
      while (e != null && e.time < time - length) {
         dequeue.remove();
         e = dequeue.peek();
      }
   }

   /**
    * Carries object with a temporal information.
    *
    * @param <E>
    *       The type of object to carry.
    */
   private static class TemporalObject<E> {

      /**
       * The object carried.
       */
      private final E object;

      /**
       * The temporal information related to the object.
       */
      private final long time;

      /**
       * Binds an object with the current time.
       *
       * @param object
       *       The object to be carried.
       */
      private TemporalObject(final E object) {
         this.object = object;
         this.time = System.currentTimeMillis();
      }

      /**
       * Binds an object with the provided artificial time.
       *
       * @param object
       *       The object to be carried.
       * @param time
       *       Artificial time.
       */
      private TemporalObject(final E object, final long time) {
         this.object = object;
         this.time = time;
      }

      /**
       * Gets the carried object.
       *
       * @return The object stored.
       */
      public E getObject() {
         return object;
      }

      /**
       * Gets the temporal information bound to the carried object.
       *
       * @return The temporal information bound to the carried object.
       */
      public long getTime() {
         return time;
      }
   }
}
