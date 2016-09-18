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
package org.perfcake.message.sequence;

import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The individual sequence values are read from external files listed as separate lines in the provided index text file.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FilesContentSequence extends FileLinesSequence {

   /**
    * The sequence's logger.
    */
   private static final Logger log = LogManager.getLogger(FileLinesSequence.class);

   /**
    * Cache of files content.
    */
   private Map<String, String> cache = new ConcurrentHashMap<>();

   /**
    * When true, the content of individual files is cached in memory.
    * This can consume significant amount of memory so use with caution.
    */
   private boolean cacheContent = true;

   /**
    * Gets whether the content of individual files is cached in the memory.
    *
    * @return True if and only if the content of individual files is cached in the memory.
    */
   public boolean isCacheContent() {
      return cacheContent;
   }

   /**
    * Sets whether the content of individual files is cached in the memory.
    *
    * @param cacheContent
    *       True if and only if the content of individual files is cached in the memory.
    * @return Instance of this to support fluent API.
    */
   public FilesContentSequence setCacheContent(final boolean cacheContent) {
      this.cacheContent = cacheContent;
      return this;
   }

   @Override
   public void publishNext(final String sequenceId, final Properties values) {
      final Properties props = new Properties();
      super.publishNext(sequenceId, props);

      final String fileName = props.getProperty(sequenceId);
      final String content;

      if (cacheContent) {
         content = cache.computeIfAbsent(fileName, this::readFile);
      } else {
         content = readFile(fileName);
      }

      if (content != null && !content.isEmpty()) {
         values.setProperty(sequenceId, content);
      } else {
         log.warn("Empty sequence value for '" + sequenceId + "'.");
      }
   }

   /**
    * Tries to obtain the content of the given file or URL as a string.
    *
    * @param fileName
    *       File name or URL to read from.
    * @return The content of the specified location or empty string when there was an error reading the location.
    */
   private String readFile(final String fileName) {
      try {
         return Utils.readLines(fileName).stream().collect(Collectors.joining());
      } catch (UncheckedIOException | IOException e) {
         log.warn("Unable to read file content for use in sequence: ", e);
      }

      return "";
   }
}
