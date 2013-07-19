package org.perfcake.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UtilsTest {

   @Test
   public void camelCaseToEnum() {
      Assert.assertEquals(Utils.camelCaseToEnum("camelCaseStringsWithACRONYMS"), "CAMEL_CASE_STRINGS_WITH_ACRONYMS");
   }
}
