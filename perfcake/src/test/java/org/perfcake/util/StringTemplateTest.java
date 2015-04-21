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
package org.perfcake.util;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Tests {@link org.perfcake.util.StringTemplate}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class StringTemplateTest {

   @Test
   public void testStringTemplateLarge() {
      final String message = StringUtils.repeat("Galia est omnis divisa in partes tres quarum unam incolunt Belgae, aliam Aquitanii, tertiam qui lingua ipsorum Celtae, nostra Gali appelantur.", 5 * 1024 / 140);
      final String expression = "${hello} ${1+1} ${ahoj||1} ${hello - 1} ${env.JAVA_HOME} ${props['java.runtime.name']}";

      final Properties vars = new Properties();
      vars.setProperty("hello", "4"); // set int as string

      final String result = StringTemplate.parseTemplate(message + expression, vars); // this passes variables into constructor immediately

      Assert.assertEquals(result, message + "4 2 1 3 " + System.getenv("JAVA_HOME") + " " + System.getProperty("java.runtime.name"));
   }

   @Test
   public void testStringTemplateBasic() {
      final String expression = "@{hello} ${1+1} @{ahoj||1} @{hello - 1} ${env.JAVA_HOME} ${props['java.runtime.name']}";
      final StringTemplate template = new StringTemplate(expression);

      final Properties vars = new Properties();
      vars.put("hello", 4); // entered directly as int - usually, we should use setProperty() here

      final String result = template.toString(vars); // pass the variables later

      Assert.assertEquals(result, "4 2 1 3 " + System.getenv("JAVA_HOME") + " " + System.getProperty("java.runtime.name"));
   }

   @Test
   public void testNoTemplate() {
      final String noTemplate = "Karel Pilka";
      final StringTemplate template = new StringTemplate(noTemplate);
      Assert.assertEquals(template.toString(), noTemplate);
      Assert.assertFalse(template.hasPlaceholders());
   }

   @Test
   public void testEscapeAtStringBeginningAndDuplicateNames() {
      // make sure that escaped pattern at the string beginning is properly ignored and that escaped duplicates are skipped too
      String expression = "\\@{hello} @{hello} \\@{hello}";
      StringTemplate template = new StringTemplate(expression);
      final Properties vars = new Properties();

      Assert.assertTrue(template.hasPlaceholders());

      vars.setProperty("hello", "42");
      String result = template.toString(vars); // pass the variables later
      Assert.assertEquals(result, "@{hello} 42 @{hello}");

      // the same for the dollar sign
      System.setProperty("test_prop", "42");
      expression = "\\${hello} ${props.test_prop} \\${hello}";
      template = new StringTemplate(expression);
      result = template.toString(vars);
      Assert.assertEquals(result, "${hello} 42 ${hello}");
   }

   @Test
   public void testPatternEscaping() {
      // make sure that escaped pattern at the string beginning is properly ignored
      System.setProperty("test_prop", "kuk");
      final String expression = "\\${hello} @{hello} ${props.test_prop} \\@{hello}";
      final StringTemplate template = new StringTemplate(expression);
      final Properties vars = new Properties();

      Assert.assertTrue(template.hasPlaceholders());

      vars.setProperty("hello", "42");
      final String result = template.toString(vars); // pass the variables later
      Assert.assertEquals(result, "${hello} 42 kuk @{hello}");
   }

   @Test
   public void testPatternEfficiency() throws NoSuchFieldException, IllegalAccessException {
      final String expression = "${1 + 1}";
      final StringTemplate template = new StringTemplate(expression);
      String result = template.toString();

      Assert.assertFalse(template.hasPlaceholders());

      Assert.assertEquals(result, "2");
      final Field templateField = StringTemplate.class.getDeclaredField("template");
      templateField.setAccessible(true);
      Assert.assertNull(templateField.get(template));

      // subsequent calls must return the same without using the template engine as we did not use any @{property}
      result = template.toString();
      Assert.assertEquals(result, "2");
   }

   @Test
   public void testNotYetDefinedProperty() {
      final String expression = "${1 + 1} @{undefined}";
      final StringTemplate template = new StringTemplate(expression);

      Assert.assertTrue(template.hasPlaceholders());
      Assert.assertEquals(template.toString(), "2 null");
   }

   @Test
   public void testSpecialChars() {
      final String function = "array.add(var, [0, 1, 2], 'name');\n";
      final String expression = "<script>$(function() {\n ${1 + 1} 'ahoj' ${ohmy} @{my}\n });</script>";
      final Properties vars = new Properties();
      vars.setProperty("ohmy", function);
      final StringTemplate template = new StringTemplate(expression, vars);
      final Properties vars2 = new Properties();
      vars2.setProperty("my", function);

      Assert.assertEquals(template.toString(vars2), "<script>$(function() {\n 2 'ahoj' " + function + " " + function + "\n });</script>");
   }

}