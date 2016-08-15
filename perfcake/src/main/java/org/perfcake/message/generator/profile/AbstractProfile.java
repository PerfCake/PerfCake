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

import org.perfcake.PerfCakeException;
import org.perfcake.common.Period;

import java.util.Map;
import java.util.TreeMap;

/**
 * Facilitates development of custom message generation profiles.
 * The only method to implement is {@link #doLoadProfile(String)}. The rest is handled by this class.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractProfile implements Profile {

   /**
    * Storage of all profile requests.
    */
   private TreeMap<Long, ProfileRequest> requests = new TreeMap<>();

   /**
    * True iff we should start from the beginning when we hit the maximal defined profile time/iteration.
    */
   private boolean autoReplay = true;

   /**
    * Maximal time value in among the request entries.
    */
   private long maxEntry = -1;

   /**
    * Registers a profile request entry with the given time.
    *
    * @param time
    *       The time to register the profile request for.
    * @param request
    *       The profile request to be registered.
    */
   protected void addRequestEntry(final long time, final ProfileRequest request) {
      requests.put(time, request);
      maxEntry = Math.max(maxEntry, time);
   }

   @Override
   public void init(final String profileSource) throws PerfCakeException {
      doLoadProfile(profileSource);
   }

   /**
    * Loads the profile entries and registers the using {@link #addRequestEntry(long, ProfileRequest)}.
    *
    * @param profileSource
    *       The source of the profile entries.
    * @throws PerfCakeException
    *       When it was not possible to initialize the profile.
    */
   abstract protected void doLoadProfile(final String profileSource) throws PerfCakeException;

   @Override
   public void setAutoReplay(final boolean autoReplay) {
      this.autoReplay = autoReplay;
   }

   @Override
   public ProfileRequest getProfile(final Period period) {
      Map.Entry<Long, ProfileRequest> entry = requests.floorEntry(autoReplay ? (period.getPeriod() % maxEntry) : period.getPeriod());

      return entry != null ? entry.getValue() : requests.firstEntry().getValue();
   }
}
