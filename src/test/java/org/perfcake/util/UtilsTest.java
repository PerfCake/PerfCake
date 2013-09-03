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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UtilsTest {

   private static final String PROPERTY_LOCATION = "property.location";
   private static final String TEST_VALUE = "test.value";
   private static final String TEST_KEY = "test.key";
   private static final String DEFAULT_VALUE = "default.value";
   private static final String TEST_KEY2 = "test.key2";

   @Test
   public void camelCaseToEnum() {
      Assert.assertEquals(Utils.camelCaseToEnum("camelCaseStringsWithACRONYMS"), "CAMEL_CASE_STRINGS_WITH_ACRONYMS");
   }
   
   @Test 
   public void testTimeToHMS() {
      
      long test1 = (12 * 3600 + 12 * 60 + 12) * 1000;
      Assert.assertEquals(Utils.timeToHMS(test1), "12:12:12");
      long test2 = (121 * 3600 + 12 * 60 + 12) * 1000;
      Assert.assertEquals(Utils.timeToHMS(test2), "121:12:12");
      long test3 = (1 * 3600 + 12 * 60 + 12) * 1000;
      Assert.assertEquals(Utils.timeToHMS(test3), "1:12:12");
   }
   
   @Test
   public void testGetProperty() {
      Assert.assertNull(Utils.getProperty(TEST_KEY));
      Assert.assertEquals(Utils.getProperty(TEST_KEY, DEFAULT_VALUE), DEFAULT_VALUE);
      
      System.setProperty(TEST_KEY, TEST_VALUE);
      
      Assert.assertEquals(Utils.getProperty(TEST_KEY, DEFAULT_VALUE), TEST_VALUE);
      
      Map<String, String> env = System.getenv();
      if (!env.isEmpty()) {
         Entry<String, String> first = env.entrySet().iterator().next();
         Assert.assertEquals(Utils.getProperty(first.getKey()), first.getValue());
      }
   }
   
   @Test
   public void testFilterProperties() throws IOException {
      String unfiltered = "text with ${test.key2} property";
      System.setProperty(TEST_KEY2, TEST_VALUE);
      
      String filtered = Utils.filterProperties(unfiltered);
      
      Assert.assertEquals(filtered, "text with test.value property");
      
   }
   
   @Test
   public void testLocationToURL() throws MalformedURLException {
      URL url1 = Utils.locationToUrl("foo", PROPERTY_LOCATION, "bar", ".bak");
      Assert.assertEquals(url1.getProtocol(), "file");
      Assert.assertEquals(url1.toExternalForm(), "file://bar/foo.bak");
      
      System.setProperty(PROPERTY_LOCATION, "barbar");
      URL url2 = Utils.locationToUrl("http://foo", PROPERTY_LOCATION, "bar", ".bak");
      Assert.assertEquals(url2.getProtocol(), "http");
      Assert.assertEquals(url2.toExternalForm(), "http://foo");
   }
}
