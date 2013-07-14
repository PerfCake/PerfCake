package org.perfcake.message.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.perfcake.PerfCakeConst;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.ReceivedMessage;
import org.perfcake.message.sender.MessageSender;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.validation.ValidatorManager;

class SenderTask implements Runnable {

   private final MessageSenderManager senderManager;
   private final List<MessageTemplate> messageStore;
   private final AtomicLong counter;
   private final boolean messageNumberingEnabled;
   private final boolean isMeasuring;
   private long count;
   private static AtomicBoolean firstSent = new AtomicBoolean(false);
   private final ReportManager reportManager;

   public SenderTask(ReportManager rm, AtomicLong counter2, MessageSenderManager senderManager, List<MessageTemplate> messageStore, boolean messageNumberingEnabled, boolean isMeasuring, long count) {
      this.reportManager = rm;
      this.senderManager = senderManager;
      this.messageStore = messageStore;
      this.counter = counter2;
      // this.responseTime = resounseTime;
      this.count = count;
      this.messageNumberingEnabled = messageNumberingEnabled;
      this.isMeasuring = isMeasuring;
   }

   @Override
   public void run() {
      Properties messageAttributes = new Properties();
      HashMap<String, String> messageHeaders = new HashMap<>();
      MessageSender sender = null;
      ReceivedMessage receivedMessage = null;
      try {
         Iterator<MessageTemplate> iterator = messageStore.iterator();
         
         // only set numbering to headers if it is enabled, later there is no change to
         // filter out the headers before sending
         String msgNumberStr = String.valueOf(counter.get());
         String msgCountStr = String.valueOf(count);
         if (messageNumberingEnabled) {
            messageHeaders.put(PerfCakeConst.MESSAGE_NUMBER_PROPERTY, msgNumberStr);
            messageHeaders.put(PerfCakeConst.COUNT_MESSAGE_PROPERTY, msgCountStr);
         }
         
         // always set the attributes, it depends on the message content whether the values
         // will be used
         messageAttributes.put(PerfCakeConst.MESSAGE_NUMBER_PROPERTY, msgNumberStr);
         messageAttributes.put(PerfCakeConst.COUNT_MESSAGE_PROPERTY, msgCountStr);
         
         firstSent.set(false);
         while (iterator.hasNext()) {
            if (counter.get() == 0 && isMeasuring && !firstSent.get()) {
               messageAttributes.put(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY, PerfCakeConst.START_VALUE);
               messageHeaders.put(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY, PerfCakeConst.START_VALUE);
               firstSent.set(true);
            } else if (counter.get() == count - 1 && !firstSent.get()) {
               messageAttributes.put(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY, PerfCakeConst.STOP_VALUE);
               messageHeaders.put(PerfCakeConst.PERFORMANCE_MESSAGE_PROPERTY, PerfCakeConst.STOP_VALUE);
               firstSent.set(true);
            }

            sender = senderManager.acquireSender();
            MessageTemplate messageToSend = iterator.next();
            Message currentMessage = messageToSend.getFilteredMessage(messageAttributes);
            long multiplicity = messageToSend.getMultiplicity();

            for (int i = 0; i < multiplicity; i++) {
               receivedMessage = new ReceivedMessage(sender.send(currentMessage, messageHeaders), messageToSend);
               ValidatorManager.addToResultMessages(receivedMessage);
            }
            
            senderManager.releaseSender(sender); // !!! important !!!
            sender = null;
         }
         
         // responseTime.addAndGet(sender.getResponseTime());
         counter.incrementAndGet();
         reportManager.reportIteration();
         // sender.close();
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (sender != null) {
            senderManager.releaseSender(sender);
         }
      }
   }
}