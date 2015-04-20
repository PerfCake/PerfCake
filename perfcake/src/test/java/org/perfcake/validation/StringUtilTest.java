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
package org.perfcake.validation;

import org.perfcake.util.StringUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class StringUtilTest {

   @Test
   public void containsIgnoreCase() {
      final String str = "Ahoj SvĚte";
      final String sub = " svě";

      Assert.assertTrue(StringUtil.containsIgnoreCase(str, sub));
      Assert.assertFalse(StringUtil.containsIgnoreCase(str, sub + sub));
   }

   @Test
   public void endsWithIgnoreCase() {
      final String str = "Ahoj SvĚte";
      final String sub = "ěte";
      final String sub2 = "svě";

      Assert.assertTrue(StringUtil.endsWithIgnoreCase(str, sub));
      Assert.assertFalse(StringUtil.endsWithIgnoreCase(str, sub2));
   }

   @Test
   public void startsWithIgnoreCase() {
      final String str = "šč Ahoj SvĚte";
      final String sub = "ŠČ aHO";
      final String sub2 = "Śč";
      final String sub3 = "Ahoj";

      Assert.assertTrue(StringUtil.startsWithIgnoreCase(str, sub));
      Assert.assertFalse(StringUtil.startsWithIgnoreCase(str, sub2));
      Assert.assertFalse(StringUtil.startsWithIgnoreCase(str, sub3));
   }

   @Test
   public void trimTest() {
      final String str = "ASDFGHJKL ahoj světe DFAAKSLDFHFKSL";
      final String strTab = "\t 'a'" + str + "\ta\"'\n";
      final String trimStr = "LKJHGFDSA";

      Assert.assertEquals(StringUtil.trim(str, trimStr), " ahoj světe ");
      Assert.assertEquals(StringUtil.trim(str, ""), str);
      Assert.assertEquals(StringUtil.trim(str, str), "");
      Assert.assertEquals(StringUtil.trim(strTab), "a'" + str + "\ta");
   }

   @Test
   public void trimLines() {
      final String str = " Ahoj,    \n" + "  \t \"toto je monstrózní test'  \n" + "\n" + "EOF";
      final String res = "Ahoj,\n" + "\"toto je monstrózní test'\n" + "EOF\n";

      Assert.assertEquals(StringUtil.trimLines(str), res);
   }
}
