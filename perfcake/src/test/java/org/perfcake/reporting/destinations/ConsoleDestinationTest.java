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
package org.perfcake.reporting.destinations;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "unit")
public class ConsoleDestinationTest {

   @Test
   public void colorsTest() throws ReportingException, NoSuchFieldException, IllegalAccessException {
      final Measurement sample = new Measurement(30, 625, 192);
      sample.set(42d);
      final ConsoleDestination destination = new ConsoleDestination();
      destination.setBackground("3");
      destination.setForeground("0");
      destination.setPrefix("---[it/s]---> ");
      destination.open();

      final Field introField = ConsoleDestination.class.getDeclaredField("intro");
      introField.setAccessible(true);
      Assert.assertEquals(introField.get(destination), "\u001B[30m\u001B[43m---[it/s]---> ");

      final Field outroField = ConsoleDestination.class.getDeclaredField("outro");
      outroField.setAccessible(true);
      Assert.assertEquals(outroField.get(destination), "\u001B[0m");

      destination.report(sample);

      destination.setBackground("");
      destination.setForeground("3");
      destination.setPrefix("");
      destination.open();
      destination.report(sample);
      Assert.assertEquals(introField.get(destination), "\u001B[33m");
      Assert.assertEquals(outroField.get(destination), "\u001B[0m");

      destination.setBackground("");
      destination.setForeground("");
      destination.setPrefix("");
      destination.open();
      destination.report(sample);
      Assert.assertEquals(introField.get(destination), "");
      Assert.assertEquals(outroField.get(destination), "");

      destination.setForeground("125,126,127");
      destination.setBackground("25,26,27");
      destination.open();
      destination.report(sample);
      Assert.assertEquals(introField.get(destination), "\u001B[38;2;125;126;127m\u001B[48;2;25;26;27m");
      Assert.assertEquals(outroField.get(destination), "\u001B[0m");

      destination.setForeground("125a,126,127");
      destination.setBackground("25,26b,27");
      destination.open();
      destination.report(sample);
      Assert.assertEquals(introField.get(destination), "");
      Assert.assertEquals(outroField.get(destination), "");
   }
}