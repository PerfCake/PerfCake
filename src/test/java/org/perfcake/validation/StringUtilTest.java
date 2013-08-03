package org.perfcake.validation;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * @author Martin Večera <marvenec@gmail.com>
 * 
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
      final String str = " Ahoj,    \n" + "  \t \"toto je monstózní test'  \n" + "\n" + "EOF";
      final String res = "Ahoj,\n" + "toto je monstózní test\n" + "EOF\n";

      Assert.assertEquals(StringUtil.trimLines(str), res);
   }
}
