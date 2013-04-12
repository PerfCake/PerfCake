/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.reporting;

/**
 * <p>
 * This class contains constants representing known subtest types that are used for describing what kind of result the result is.
 * </p>
 * <p>
 * To better understand this please consult data model of Reporting and get familiar with how results are stored.
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class MeasurementTypes {
   /**
    * Average response time
    */
   public static final String AR = "AR_TOTAL";

   /**
    * AI means average iterations.
    * 
    * TOTAL - total average iterations/s CURRET - iterations/s in current time
    * window
    */
   public static final String AI_TOTAL = "AT_TOTAL";

   public static final String AI_CURRENT = "AT_CURRENT";

   /**
    * M - memory tests located at KPIReporter
    * 
    * TOTAL - total memory USED - total minus free memory REGRESSSION -
    * regression factor computed
    */
   public static final String M_TOTAL = "M_TOTAL";

   public static final String M_USED = "M_USED";

   public static final String M_REGRESSION = "M_REGRESSION";

   public static final String INSERT_FIND = "INSERT_FIND";

   public static final String INSERT_REGISTER = "INSERT_REGISTER";

   public static final String SERVICE_DELETE_UNREGISTER = "SERVICE_DELETE_UNREGISTER";

   public static final String EPR_DELETE_UNREGISTER = "EPR_DELETE_UNREGISTER";

   public static final String THREADED_FIND = "THREADED_FIND";

   public static final String PUBLISH_FIND_DELETE_REGISTER = "PFD_REGISTER";

   public static final String PUBLISH_FIND_DELETE_FIND = "PFD_FIND";

   public static final String PUBLISH_FIND_DELETE_UNREGISTER = "PFD_UNREGISTER";
}
