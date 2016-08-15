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
package org.perfcake.message.generator.profile;

/**
 * Carries the information about current requested messages generation profile.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public final class ProfileRequest {

   /**
    * The number of threads requested to execute sender tasks.
    */
   private final int threads;

   /**
    * The requested speed of sending messages in messages per second.
    */
   private final int speed;

   /**
    * Creates an immutable instance of the message generation profile.
    *
    * @param threads
    *       The number of threads requested to execute sender tasks.
    * @param speed
    *       The requested speed of sending messages in messages per second.
    */
   public ProfileRequest(final int threads, final int speed) {
      this.threads = threads;
      this.speed = speed;
   }

   /**
    * Gets the number of threads requested to execute sender tasks.
    *
    * @return The number of threads requested to execute sender tasks.
    */
   public int getThreads() {
      return threads;
   }

   /**
    * Gets the requested speed of sending messages in messages per second.
    *
    * @return The requested speed of sending messages in messages per second.
    */
   public int getSpeed() {
      return speed;
   }

   @Override
   public String toString() {
      return "ProfileRequest{" +
            "threads=" + threads +
            ", speed=" + speed +
            '}';
   }
}
