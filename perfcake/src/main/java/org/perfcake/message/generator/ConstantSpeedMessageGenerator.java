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
package org.perfcake.message.generator;

/**
 * A generator that tries to achieve given speed of messages per second.
 * Uses the underlying buffer of {@link SenderTask SenderTasks} in {@link DefaultMessageGenerator}.
 * This buffer smoothens the changes in the speed. If you need the generator to change its speed
 * more aggressively, configure {@link DefaultMessageGenerator#setSenderTaskQueueSize(int)}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConstantSpeedMessageGenerator extends DefaultMessageGenerator {

   /**
    * The desired constant speed in messages per second.
    */
   private int speed = 5000;

   /**
    * Buffer of recent times when a task was added. Its size is equal to the speed, so the smallest
    * time in the buffer (the oldest record) should be around 1000ms from now. Smaller time difference
    * means that we are too fast. Larger time difference means we are too slow.
    */
   private long[] buffer;

   /**
    * Pointer to the buffer - we use it as a circular buffer.
    */
   private int pointer = 0;

   /**
    * Duration of a break between two submitted tasks in case the speed is lower than 1000 messages per second.
    */
   private int breakDuration;

   private long getSmallest() {
      return buffer[(pointer + 1) % speed];
   }

   private long getLast() {
      return buffer[pointer];
   }

   private void addTime(final long time) {
      pointer = (pointer + 1) % speed;
      buffer[pointer] = time;
   }

   @Override
   protected boolean prepareTask() throws InterruptedException {
      long currentTime = System.currentTimeMillis();

      // the size of the buffer = speed, so the smallest time should be around one second old
      // in case of a non-zero breakDuration, we cannot submit a task before the break passed
      if (currentTime - getSmallest() > 1000 && (!(breakDuration > 0) || currentTime - breakDuration >= getLast())) {
         boolean res = super.prepareTask();
         if (res) {
            addTime(currentTime);
         }

         return res;
      }

      return false;
   }

   /**
    * Gets the desired constant speed in messages per second.
    *
    * @return The speed.
    */
   public int getSpeed() {
      return speed;
   }

   /**
    * Sets the desired constant speed in messages per second.
    *
    * @param speed
    *       The speed in messages per second.
    */
   public void setSpeed(final int speed) {
      this.speed = speed;
      buffer = new long[speed];

      for (int i = 0; i < speed; i++) {
         buffer[i] = 0;
      }

      breakDuration = 1000 / speed; // will be 0 for speed > 1000
   }
}
