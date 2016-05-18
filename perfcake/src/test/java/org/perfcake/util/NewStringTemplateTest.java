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
package org.perfcake.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * Tests {@link StringTemplate}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class NewStringTemplateTest {

   private static Properties props(final String... pairs) {
      final Properties props = new Properties();

      int i = 0;
      while (i < pairs.length) {
         if (i + 1 < pairs.length) {
            props.setProperty(pairs[i++], pairs[i++]);
         }
      }

      return props;
   }

   @DataProvider(name = "patterns")
   public static Object[][] testPatterns() {
      return new Object[][] {
            { "", "", props("", ""), props("", "") },
            { "${abc}", "123", props("abc", "123"), props("", "") },
            { "@{abc}", "null", props("abc", "123"), props("", "") }
      };
   }

   @Test(dataProvider = "patterns")
   public void testStringTemplateLarge(final String template, final String result, final Properties permanentProps, final Properties dynamicProps) {
      // s = new StringTemplate(template, permanentProps)
      // Assert.equals(s.render(dynamicProps), result)

   }

}
