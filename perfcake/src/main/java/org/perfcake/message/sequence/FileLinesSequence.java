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
package org.perfcake.message.sequence;

import org.perfcake.PerfCakeException;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Every single line in a given input file specifies a value of this sequence.
 * Once the end of the file is hit, the sequence starts from beginning.
 * The whole file is read in the memory, so make sure the file is of a reasonable size
 * given your expectations and memory limits.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FileLinesSequence implements Sequence {

   /**
    * The generator's logger.
    */
   private static final Logger log = LogManager.getLogger(FileLinesSequence.class);

   /**
    * The location of the file to read ro
    */
   private String fileUrl;

   /**
    * Content of the input file as an array of lines.
    */
   private String[] lines;

   /**
    * Current position in the array of {@link #lines}. We use this primitive approach to be easily thread-safe.
    * It is a dirty trick but does the job. Using AtomicInteger would lead to another synchronization.
    */
   private final Integer[] iterator = new Integer[] { 0 };

   @Override
   public String getNext() {
      synchronized (iterator) {
         if (iterator[0] >= lines.length) {
            iterator[0] = 0;
         }

         return lines[iterator[0]++];
      }
   }

   @Override
   public void reset() throws PerfCakeException {
      try {
         final List<String> linesArray = Utils.readLines(new URL(fileUrl));
         lines = linesArray.toArray(new String[linesArray.size()]);
         iterator[0] = 0;
      } catch (IOException e) {
         log.warn(String.format("Could not initialize file lines sequence for file %s: ", fileUrl), e);
         throw new PerfCakeException(e);
      }
   }

   public String getFileUrl() {
      return fileUrl;
   }

   public void setFileUrl(final String fileUrl) {
      this.fileUrl = fileUrl;
   }
}
