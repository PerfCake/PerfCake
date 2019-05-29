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
package org.perfcake;

import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TestUtil {

   /**
    * Creates new instance of {@link Properties} based on the couples of strings passed to the method.
    * In case of odd number of arguments, the last one is ignored.
    *
    * @param pairs
    *       The arguments to create properties like arg0->arg1, arg2->arg3, ...
    * @return The properties created according to the arguments.
    */
   public static Properties props(final String... pairs) {
      final Properties props = new Properties();

      int i = 0;
      while (i < pairs.length) {
         if (i + 1 < pairs.length) {
            props.setProperty(pairs[i++], pairs[i++]);
         }
      }

      return props;
   }
}
