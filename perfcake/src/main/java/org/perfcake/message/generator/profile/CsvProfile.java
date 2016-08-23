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
package org.perfcake.message.generator.profile;

import org.perfcake.PerfCakeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.LongAdder;

/**
 * Loads a message generation profile from a CSV file in the format <code>&lt;time&gt;;&lt;threads&gt;;&lt;speed&gt;</code>.
 * The entries are sorted. The time can be either an iteration number or a time in milliseconds depending on the test scenario
 * definition (run defined by number of iterations or by time).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CsvProfile extends AbstractProfile {

   @Override
   protected void doLoadProfile(final String profileSource) throws PerfCakeException {
      try {
         final LongAdder lineNumber = new LongAdder();
         final StringBuilder errors = new StringBuilder();

         Files.lines(Paths.get(profileSource)).forEach(line -> {
            lineNumber.increment();
            final String[] items = line.split(";");

            if (items.length > 3) {
               errors.append("Invalid format of CSV file (too many entries) ").append(profileSource).append(" on line ").append(lineNumber.intValue()).append(".\n");
            } else {
               try {
                  addRequestEntry(Long.parseLong(items[0]), new ProfileRequest(Integer.parseInt(items[1]), Double.parseDouble(items[2])));
               } catch (NumberFormatException nfe) {
                  errors.append("Invalid format of CSV file (not a number) ").append(profileSource).append(" on line ").append(lineNumber.intValue()).append(".\n");
               }
            }
         });

         if (errors.length() != 0) {
            throw new PerfCakeException(errors.toString());
         }
      } catch (IOException e) {
         throw new PerfCakeException("Unable to read profile from CSV file " + profileSource + ": ", e);
      }
   }
}
