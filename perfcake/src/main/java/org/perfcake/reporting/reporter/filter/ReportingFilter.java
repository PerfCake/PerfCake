/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package org.perfcake.reporting.reporter.filter;

import org.perfcake.reporting.Measurement;

import java.util.Optional;

/**
 * Can filter passing {@link org.perfcake.reporting.Measurement Measurements} in order to lower
 * the number of reported data especially in long running performance tests.
 *
 * It is the responsibility of a filter instance to cache any data it needs for further processing.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface ReportingFilter {

   /**
    * Filters the passing {@link Measurement Measurements}. Only returns a value when a {@link Measurement}
    * should be actually reported.
    *
    * @param measurement
    *       Data to be processed and possibly filtered.
    * @return {@link Optional#empty()} when no data should be reported or data to be reported.
    */
   Optional<Measurement> filter(final Measurement measurement);
}
