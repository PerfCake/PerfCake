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

import org.testng.Assert;
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
            // { "", "", props("", ""), props("", "") },
            { "${abc}", "123", props("abc", "123"), null },
            { "${abc}", "null", null, props("abc", "123") },
            { "@{abc}", "123", props("abc", "123"), null },
            { "@{abc}", "123", null, props("abc", "123") },
            { "$ab@cd", "$ab@cd", props("ab", "1", "cd", "2"), null },
            { "a$ab@cda", "a$ab@cda", props("ab", "1", "cd", "2"), null },
            { "\\\\@{cd}", "\\2", null, props("cd", "2") },
            { "\\${ab}\\@{cd}", "${ab}@{cd}", props("ab", "1", "cd", "2"), props("ab", "1", "cd", "2") },
            { "a\\$ab\\@cd", "a$ab@cd", props("ab", "1", "cd", "2"), props("ab", "1", "cd", "2") },
            { "a\\\\${ab}\\\\@{cd}", "a\\1\\2", props("ab", "1", "cd", "2"), props("ab", "1", "cd", "2") },
            { "a\\\\${ab}\\\\@{cd}", "a\\1\\2", props("ab", "1"), props("cd", "2") },
            { "a\\\\${ab}\\\\@{cd}", "a\\null\\2", props("cd", "2"), props("ab", "1") },
            { "a\\$ab\\@cd", "a$ab@cd", props("ab", "1", "cd", "2"), props("ab", "1", "cd", "2") },
            { "${ab}${ahoj:4}@{cd:1}${env.JAVA_HOME}${props['java.runtime.name']}", "142" + System.getenv("JAVA_HOME") + System.getProperty("java.runtime.name"), props("ab", "1"), props("cd", "2") },
            { "@{ab}${cd:4}${ab:7}@{env.JAVA_HOME}@{props['java.runtime.name']}", "141" + System.getenv("JAVA_HOME") + System.getProperty("java.runtime.name"), props("ab", "1"), props("cd", "2") },
            { "${env.JAVA_HOME} - ${env.JAVA_HOME:aaa} - ${env.nonexist:a$a\\\\a@a\\$a\\@a{a\\{a\\}a:a} - \\${env.JAVA_HOME} - ${...",
                  System.getenv("JAVA_HOME") + " - " + System.getenv("JAVA_HOME") + " - a$a\\a@a$a@a{a{a}a:a - ${env.JAVA_HOME} - ${...", null, null },
            { "${aaa\\}", "${aaa}", null, null },
            { "\\\\${env.JAVA_HOME} \\$aa", "\\" + System.getenv("JAVA_HOME") + " $aa", null, null },
            { "${props.java.runtime.name} ${props[java.runtime.name]} ${props['java.runtime.name']}", System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.name"), null, null },
            { "@{aaa:@{\\@\\{\\}\\\\}", "@{@{\\}", null, null },
      };
   }

   @Test(dataProvider = "patterns")
   public void testStringTemplateLarge(final String template, final String result, final Properties permanentProps, final Properties dynamicProps) {
      NewStringTemplate s = new NewStringTemplate(template, permanentProps);
      Assert.assertEquals(s.toString(dynamicProps), result, String.format("Original template: '%s' perm: %s, dynamic: %s", template, permanentProps == null ? null : permanentProps.toString(),
            dynamicProps == null ? null : dynamicProps.toString()));
   }

   @Test
   public void basicTest() {
      NewStringTemplate t = new NewStringTemplate("\\\\@{cd}");
      System.out.println(t.toString(props("cd", "2")));
   }

}
