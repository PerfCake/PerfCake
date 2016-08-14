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

/**
 * A custom message generation profile that specifies dynamic performance test parameters (no. of threads and speed).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Profile {

   /**
    * Initializes the profile, e.g. reads data from an external file.
    *
    * @param profileSource
    *       Where to read the profile data from. Can be unused by some profiles.
    * @throws PerfCakeException
    *       When it was not possible to initialize the profile.
    */
   void init(final String profileSource) throws PerfCakeException;

   /**
    * Sets whether we should start from the beginning when we hit the maximal defined profile time/iteration.
    *
    * @param autoReplay
    *       True if and only if we should start from the beginning.
    */
   void setAutoReplay(final boolean autoReplay);

   /**
    * Gets the messages generation profile for the given time period.
    *
    * @param period
    *       The current performance test progress.
    * @return The requested profile for the given period.
    */
   ProfileRequest getProfile(final Period period);
}
