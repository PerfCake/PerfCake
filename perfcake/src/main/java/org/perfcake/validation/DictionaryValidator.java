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

import org.perfcake.message.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Creates a dictionary of valid responses and use this to validate them in another run.
 * It is also possible to create the dictionary manually, however, this is to complicated task and we always
 * recommend running the validation in record mode first. Any manual changes can be done later.
 * Dictionary validator creates an index file and a separate file for each response. A writable directory must
 * be specified. The default index file name can be redefined. The response file names are based on hash codes of
 * the original messages. Empty, null or equal messages will overwrite the file but this is not the intended use
 * of this validator. Index file is never overwritten, if you really insist on recreating it, please rename or
 * delete the file manually (this is for safety reasons).
 * It is not sufficient to store just the index as it is likely that the correct messages will be manually
 * modified after they are recorded.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DictionaryValidator implements MessageValidator {

   /**
    * A logger for this class.
    */
   private final Logger log = LogManager.getLogger(ValidationManager.class);

   /**
    * The directory where the dictionary is/will be store.
    */
   private String dictionaryDirectory;

   /**
    * The file name of the dictionary index.
    */
   private String dictionaryIndex = "index";

   /**
    * Is the record mode active?
    */
   private boolean record = false;

   /**
    * Did we check the existence of the directory index? We never ever allow its overwrite in the record mode.
    */
   private boolean indexChecked = false;

   /**
    * Cached directory index.
    */
   private Properties indexCache;

   /**
    * Escapes characters <code>=</code> and <code>:</code> in the payload string.
    *
    * @param payload
    *       The payload string to be escaped.
    * @return Escaped payload.
    */
   private String escapePayload(final String payload) {
      return payload.replaceAll("[\\n\\r\\\\:= ]", "_");
   }

   /**
    * Records the correct response.
    *
    * @param originalMessage
    *       The original message.
    * @param response
    *       The response that is considered correct.
    * @throws ValidationException
    *       If any of the disk operations fails.
    */
   private void recordResponse(final Message originalMessage, final Message response) throws ValidationException {
      final String responseHashCode = Integer.toString(response.getPayload().toString().hashCode());
      final File targetFile = new File(dictionaryDirectory, responseHashCode);
      if (targetFile.exists()) {
         throw new ValidationException(String.format("Target file for the message hash code '%s' already exists. Probably a duplicate original message.", responseHashCode));
      }

      try (final Writer indexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getIndexFile(), true), StandardCharsets.UTF_8));
            final Writer responseWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
         indexWriter.append(escapePayload(originalMessage.getPayload().toString()));
         indexWriter.append("=");
         indexWriter.append(responseHashCode);
         indexWriter.append("\n");

         responseWriter.write(response.getPayload().toString());
      } catch (final IOException e) {
         throw new ValidationException(String.format("Cannot record correct response for message '%s': ", response.getPayload().toString()), e);
      }
   }

   /**
    * Reads the index into memory, or returns the previously read index.
    *
    * @return The response index.
    * @throws ValidationException
    *       If any of the disk operations fails.
    */
   private Properties getIndexCache() throws ValidationException {
      if (indexCache == null) {
         indexCache = new Properties();
         try (final Reader indexReader = new BufferedReader(new InputStreamReader(new FileInputStream(getIndexFile()), StandardCharsets.UTF_8))) {
            indexCache.load(indexReader);
         } catch (final IOException e) {
            throw new ValidationException(String.format("Unable to load index file '%s': ", getIndexFile().getAbsolutePath()), e);
         }
      }

      return indexCache;
   }

   /**
    * Validates the response against the previously recorded correct responses.
    *
    * @param originalMessage
    *       The original message.
    * @param response
    *       The response to be validated.
    * @return True if and only if the validation passed.
    * @throws ValidationException
    *       If any of the disk operations fails.
    */
   private boolean validateResponse(final Message originalMessage, final Message response) throws ValidationException {
      final String responseHashCode = getIndexCache().getProperty(escapePayload(originalMessage.getPayload().toString()));
      if (responseHashCode == null) { // we do not have any such message
         return false;
      }

      try {
         final String newResponse = response != null && response.getPayload() != null ? response.getPayload().toString() : "";
         final String responseString = new String(Files.readAllBytes(Paths.get(dictionaryDirectory, responseHashCode)), StandardCharsets.UTF_8);

         return newResponse.equals(responseString);
      } catch (final IOException e) {
         throw new ValidationException(String.format("Cannot read correct response from file '%s': ", new File(dictionaryDirectory, responseHashCode).getAbsolutePath()), e);
      }
   }

   /**
    * Gets the file with index of recorded responses.
    *
    * @return The index file.
    */
   private File getIndexFile() {
      return new File(dictionaryDirectory, dictionaryIndex);
   }

   /**
    * Checks whether the index file exists.
    *
    * @return <code>true</code> if and only if the index file exists.
    */
   private boolean indexExists() {
      return (dictionaryDirectory != null && dictionaryIndex != null) && getIndexFile().exists();
   }

   @Override
   public boolean isValid(final Message originalMessage, final Message response, final Properties messageAttributes) {
      if (!indexChecked && record && indexExists()) {
         // We are in record mode and did not previously check for the index existence. Once this is checked and the index is not present,
         // we never appear here again. If the check did not pass, we will log the error again and again.
         log.error("Error while trying to record responses - index file already exists, overwrite not permitted.");
         return false;
      } else {
         indexChecked = true;

         if (record) {
            try { // in record mode record the answer (considered as correct)
               recordResponse(originalMessage, response);

               return true;
            } catch (final ValidationException e) {
               log.error("Error recording correct response: ", e);
            }
         } else {
            try { // in normal mode, validate the answer against already recorded responses
               return validateResponse(originalMessage, response);
            } catch (final ValidationException e) {
               log.error("Error validating response: ", e);
            }
         }

      }

      return false;
   }

   /**
    * Gets the dictionary directory name.
    *
    * @return The dictionary directory name.
    */
   public String getDictionaryDirectory() {
      return dictionaryDirectory;
   }

   /**
    * Sets Gets the dictionary directory name.
    *
    * @param dictionaryDirectory
    *       The name of the dictionary directory.
    * @return Instance of this for fluent API.
    */
   public DictionaryValidator setDictionaryDirectory(final String dictionaryDirectory) {
      this.dictionaryDirectory = dictionaryDirectory;
      return this;
   }

   /**
    * Gets the file name of the dictionary index.
    *
    * @return The file name of the dictionary index.
    */
   public String getDictionaryIndex() {
      return dictionaryIndex;
   }

   /**
    * Sets the file name of the dictionary index.
    *
    * @param dictionaryIndex
    *       The file name of the dictionary index.
    * @return Instance of this for fluent API.
    */
   public DictionaryValidator setDictionaryIndex(final String dictionaryIndex) {
      this.dictionaryIndex = dictionaryIndex;
      return this;
   }

   /**
    * Checks whether we are in the record mode.
    *
    * @return True if and only if the record mode is active.
    */
   public boolean isRecord() {
      return record;
   }

   /**
    * Sets the record mode.
    *
    * @param record
    *       <code>true</code> to activate the record mode.
    * @return Instance of this for fluent API.
    */
   public DictionaryValidator setRecord(final boolean record) {
      this.record = record;
      return this;
   }
}
