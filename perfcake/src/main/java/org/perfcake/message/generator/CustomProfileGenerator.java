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
package org.perfcake.message.generator;

import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.generator.profile.Profile;
import org.perfcake.message.generator.profile.ProfileRequest;
import org.perfcake.util.properties.MandatoryProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generates the messages according to provided custom profile.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CustomProfileGenerator extends ConstantSpeedMessageGenerator {

   /**
    * The generator's logger.
    */
   private static final Logger log = LogManager.getLogger(CustomProfileGenerator.class);

   /**
    * The class name of the profile.
    */
   @MandatoryProperty
   private String profileClass;

   /**
    * Where to read the profile data from. Can be unused by some profiles.
    */
   private String profileSource;

   /**
    * True iff we should start from the beginning when we hit the maximal defined profile time/iteration.
    */
   private boolean autoReplay = true;

   /**
    * The profile instance.
    */
   private Profile profile;

   /**
    * When did we reconfigured the generator for the last time.
    */
   private long lastTime = -1;

   /**
    * Last speed when we did the reconfiguration.
    */
   private double lastSpeed = -1;

   /**
    * Last number of threads when we did the reconfiguration.
    */
   private int lastThreads = -1;

   @Override
   public void generate() throws Exception {
      setSpeed(getSpeed()); // initializes internal structures

      if (!profileClass.contains(".")) {
         profileClass = "org.perfcake.message.generator.profile." + profileClass;
      }

      profile = (Profile) Class.forName(profileClass).getDeclaredConstructor().newInstance();
      profile.init(profileSource);
      profile.setAutoReplay(autoReplay);

      super.generate();
   }

   @Override
   protected boolean prepareTask() throws InterruptedException {
      final long currentTime = runInfo.getRunTime();
      if (currentTime != lastTime) {
         lastTime = currentTime;
         reconfigure(profile.getProfile(getCurrentPeriod()));
      }

      return super.prepareTask();
   }

   /**
    * Reconfigures the generator according to the latest profile request.
    *
    * @param request
    *       The latest profile request.
    */
   private void reconfigure(final ProfileRequest request) {
      if (request != null) {
         if (lastThreads != request.getThreads()) {
            lastThreads = request.getThreads();
            setThreads(request.getThreads());
            executorService.setCorePoolSize(request.getThreads());
            executorService.setMaximumPoolSize(request.getThreads());
         }

         if (lastSpeed != request.getSpeed()) {
            lastSpeed = request.getSpeed();
            setSpeed(request.getSpeed());
         }
      }
   }

   /**
    * Gets the current performance test progress.
    *
    * @return The current performance test progress.
    */
   private Period getCurrentPeriod() {
      if (runInfo.getDuration().getPeriodType() == PeriodType.ITERATION) {
         return new Period(PeriodType.ITERATION, runInfo.getIteration());
      } else {
         return new Period(PeriodType.TIME, runInfo.getRunTime());
      }
   }

   /**
    * Gets the class name of the profile.
    *
    * @return The class name of the profile.
    */
   public String getProfileClass() {
      return profileClass;
   }

   /**
    * Sets the class name of the profile.
    *
    * @param profileClass
    *       The class name of the profile.
    * @return Instance of this to support fluent API.
    */
   public CustomProfileGenerator setProfileClass(final String profileClass) {
      this.profileClass = profileClass;
      return this;
   }

   /**
    * Gets where to read the profile data from. Can be unused by some profiles.
    *
    * @return Where to read the profile data from.
    */
   public String getProfileSource() {
      return profileSource;
   }

   /**
    * Sets where to read the profile data from. Can be unused by some profiles.
    *
    * @param profileSource
    *       Where to read the profile data from.
    * @return Instance of this to support fluent API.
    */
   public CustomProfileGenerator setProfileSource(final String profileSource) {
      this.profileSource = profileSource;
      return this;
   }

   /**
    * Gets whether we should start from the beginning when we hit the maximal defined profile time/iteration.
    *
    * @return True if and only if we should start from the beginning.
    */
   public boolean isAutoReplay() {
      return autoReplay;
   }

   /**
    * Sets whether we should start from the beginning when we hit the maximal defined profile time/iteration.
    *
    * @param autoReplay
    *       True if and only if we should start from the beginning.
    * @return Instance of this to support fluent API.
    */
   public CustomProfileGenerator setAutoReplay(final boolean autoReplay) {
      this.autoReplay = autoReplay;
      return this;
   }
}
