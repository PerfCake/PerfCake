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

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.ReceivedMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Validates message responses returned by {@link org.perfcake.message.sender.MessageSender}
 * using a set of {@link org.perfcake.validation.MessageValidator} instances.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:lucie.fabrikova@gmail.com">Lucie Fabriková</a>
 */
public class ValidationManager {

   private static final String OVERALL_STAT_KEY = "overall";

   /**
    * A map of validators: validator id => validator instance.
    */
   private final Map<String, MessageValidator> validators = new TreeMap<>();

   /**
    * A logger.
    */
   private final Logger log = LogManager.getLogger(ValidationManager.class);

   /**
    * An internal thread that takes one response after another and validates them.
    */
   private Thread validationThread = null;

   /**
    * Indicates whether the validation is finished. Starts with true as there is no validation running at the beginning.
    */
   private boolean finished = true;

   /**
    * Were all messages validated properly so far?
    */
   private boolean allMessagesValid = true;

   /**
    * Is validation enabled?
    */
   private boolean enabled = false;

   /**
    * Unless specified in the scenario, the validation thread has some sleep for it not to influence measurement.
    * At the end, when there is nothing else to do, we can go through the remaining responses faster.
    */
   private boolean fastForward = false;

   /**
    * When true, the validation thread just waits for the input queue to become empty and ends.
    */
   private boolean expectLastMessage = false;

   /**
    * A queue with the validation tasks.
    */
   private Queue<ValidationTask> validationTasks;

   /**
    * Stores validation results statistics.
    */
   private final Map<String, Score> statistics = new HashMap<>();

   /**
    * Creates a new validator manager. The message responses are store in a file queue in a temporary file.
    *
    * @throws PerfCakeException
    *       When it was not possible to initialize the message store.
    */
   public ValidationManager() throws PerfCakeException {
      statistics.put(OVERALL_STAT_KEY, new Score());
      try {
         final File tmpFile = File.createTempFile("perfcake", "queue");
         tmpFile.deleteOnExit();
         setQueueFile(tmpFile);
      } catch (final IOException e) {
         throw new PerfCakeException("Cannot create a file queue for messages to be validated: ", e);
      }
   }

   /**
    * Sets a different location of the file queue for storing message responses.
    *
    * @param queueFile
    *       The new location of the file queue.
    * @throws PerfCakeException
    *       When it was not possible to initialize the file queue or there is a running validation.
    */
   public void setQueueFile(final File queueFile) throws PerfCakeException {
      if (isFinished()) {
         validationTasks = new FileQueue<ValidationTask>(queueFile);
      } else {
         throw new PerfCakeException("It is not possible to change the file queue while there is a running validation.");
      }
   }

   /**
    * Adds a new message validator.
    *
    * @param validatorId
    *       A string id of the new validator.
    * @param messageValidator
    *       A validator instance.
    */
   public void addValidator(final String validatorId, final MessageValidator messageValidator) {
      validators.put(validatorId, messageValidator);
   }

   /**
    * Gets the validator with the given id.
    *
    * @param validatorId
    *       A string id of the validator.
    * @return The validator instance or null if there is no such validator with the given id.
    */
   public MessageValidator getValidator(final String validatorId) {
      return validators.get(validatorId);
   }

   /**
    * Get all the validators requested in the list of ids.
    *
    * @param validatorIds
    *       A list of ids of validators to be returned.
    * @return The list of requested validators.
    */
   public List<MessageValidator> getValidators(final List<String> validatorIds) {
      final List<MessageValidator> _validators = new ArrayList<>();

      for (final String id : validatorIds) {
         _validators.add(getValidator(id));
      }

      return _validators;
   }

   /**
    * Starts the validation process. This mainly means starting a new validator thread.
    */
   public void startValidation() {
      if (validationThread == null || !validationThread.isAlive()) {
         expectLastMessage = false;
         validationThread = new Thread(new ValidationThread());
         validationThread.setDaemon(true); // we do not want to block JVM
         validationThread.start();
      }
   }

   /**
    * Wait for the validation to be finished. The call is blocked until the validator thread finishes execution or an exception
    * is thrown. Internally, this joins the validator thread to the current thread.
    *
    * @throws InterruptedException
    *       If the validator thread was interrupted.
    */
   public void waitForValidation() throws InterruptedException {
      if (validationThread != null) {
         fastForward = true;
         expectLastMessage = true;
         validationThread.join();
      }
   }

   /**
    * Interrupts the validator thread immediately. There might be remaining unfinished validations.
    */
   public void terminateNow() {
      if (validationThread != null) {
         validationThread.interrupt();
      }
   }

   /**
    * Submits a new validation task. The message response in it will be validated.
    *
    * @param validationTask
    *       The new validation task to be processed.
    */
   public void submitValidationTask(final ValidationTask validationTask) {
      validationTasks.add(validationTask);
   }

   /**
    * Gets the number of messages that needs to be validated.
    *
    * @return The current size of the file queue with messages waiting for validation.
    */
   public int messagesToBeValidated() {
      return validationTasks.size();
   }

   /**
    * Is validation facility enabled?
    *
    * @return <code>true</code> if validation is enabled.
    */
   public boolean isEnabled() {
      return enabled;
   }

   /**
    * Enables/disables validation. This only takes effect before the validation is started and or finished.
    *
    * @param enabled
    *       Specifies whether we want the validation to be enabled.
    */
   public void setEnabled(final boolean enabled) {
      if (enabled || finished) {
         this.enabled = enabled;
      } else {
         log.error("Validation cannot be disabled while the validation is in progress.");
      }
   }

   /**
    * Determines whether the validation process finished already.
    *
    * @return <code>true</code> if the validation finished or was not started yet.
    */
   public boolean isFinished() {
      return finished;
   }

   /**
    * Determines whether the validation process is performed in a fast forward mode.
    * <p>Unless specified in the scenario, the validation thread has some sleep for it not to influence measurement.
    * At the end, when there is nothing else to do, we can go through the remaining responses faster.</p>
    *
    * <p>The fast forward mode removes the sleep.</p>
    *
    * @return <code>true</code> if the sleep period is disabled.
    */
   public boolean isFastForward() {
      return fastForward;
   }

   /**
    * Enables/disables the fast forward mode of the validation.
    *
    * @param fastForward
    *       <code>true</code> to enable the fast forward mode.
    */
   public void setFastForward(final boolean fastForward) {
      this.fastForward = fastForward;
   }

   public boolean isAllMessagesValid() {
      return allMessagesValid;
   }

   private void logStatistics() {
      final StringBuilder sb = new StringBuilder("=== Validation Statistics ===\n");
      final Score total = statistics.get(OVERALL_STAT_KEY);
      sb.append("Overall validated ").append(total.getPassed() + total.getFailed()).append(" messages of which ");
      sb.append(total.getPassed()).append(" passed and ");
      sb.append(total.getFailed()).append(" failed.\n");

      for (final Map.Entry<String, Score> entry : statistics.entrySet()) {
         final String key = entry.getKey();
         final Score value = entry.getValue();

         if (!OVERALL_STAT_KEY.equals(key)) {
            sb.append("Thread [").append(key).append("]: Totally validated ").append(value.getFailed() + value.getPassed());
            sb.append(" messages of which ").append(value.getPassed()).append(" passed and ").append(value.getFailed()).append(" failed.\n");
         }
      }

      sb.append("End of statistics.");

      log.info(sb.toString());
   }

   /**
    * Gets the overall validation statistics.
    *
    * @return The overall statistics score.
    */
   protected Score getOverallStatistics() {
      return statistics.get(OVERALL_STAT_KEY);
   }

   protected static final class Score {
      private long passed = 0;
      private long failed = 0;

      public long getPassed() {
         return passed;
      }

      public void incPassed() {
         passed = passed + 1;
      }

      public long getFailed() {
         return failed;
      }

      public void incFailed() {
         failed = failed + 1;
      }

      public String toString() {
         return String.format("Score: total %d, passed %d, failed %d", passed + failed, passed, failed);
      }
   }

   /**
    * Represents the internal validator thread. The thread validates one message with all registered validators and then
    * sleeps for 500ms. This is needed for the validation not to influence measurement. After a call to {@link #waitForValidation()} the
    * sleeps are skipped.
    */
   private class ValidationThread implements Runnable {

      @Override
      public void run() {
         boolean isMessageValid;
         ReceivedMessage receivedMessage;
         ValidationTask validationTask;
         finished = false;
         allMessagesValid = true;

         if (validators.isEmpty()) {
            log.warn("No validators set in scenario.");
            return;
         }

         try {
            while (!validationThread.isInterrupted() && (!expectLastMessage || !validationTasks.isEmpty())) {
               validationTask = validationTasks.poll();
               receivedMessage = null;

               if (validationTask != null) {
                  receivedMessage = validationTask.getReceivedMessage();

                  for (final MessageValidator validator : getValidators(receivedMessage.getSentMessageTemplate().getValidatorIds())) {
                     isMessageValid = validator.isValid(receivedMessage.getSentMessage(), new Message(receivedMessage.getResponse()), receivedMessage.getMessageAttributes());
                     if (log.isTraceEnabled()) {
                        log.trace(String.format("Message response %s validated with %s returns %s.", receivedMessage.getResponse().toString(), validator.toString(), String.valueOf(isMessageValid)));
                     }

                     if (log.isInfoEnabled()) {
                        if (isMessageValid) {
                           statistics.get(OVERALL_STAT_KEY).incPassed();

                           Score score = statistics.get(validationTask.getThreadName());
                           if (score == null) {
                              score = new Score();
                              statistics.put(validationTask.getThreadName(), score);
                           }

                           score.incPassed();
                        } else {
                           statistics.get(OVERALL_STAT_KEY).incFailed();

                           Score score = statistics.get(validationTask.getThreadName());
                           if (score == null) {
                              score = new Score();
                              statistics.put(validationTask.getThreadName(), score);
                           }

                           score.incFailed();
                        }
                     }

                     allMessagesValid &= isMessageValid;
                  }
               }
               if (!fastForward || receivedMessage == null) {
                  Thread.sleep(500); // we do not want to block senders
               }
            }
         } catch (final InterruptedException ex) {
            // never mind, we have been asked to terminate
         }

         if (log.isInfoEnabled()) {
            logStatistics();
            log.info("The validator thread finished with the result: " + (allMessagesValid ? "all messages are valid." : "there were validation errors."));
         }

         finished = true;
      }
   }
}
