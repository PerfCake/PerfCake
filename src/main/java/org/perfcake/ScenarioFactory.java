package org.perfcake;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.model.Header;
import org.perfcake.model.Property;
import org.perfcake.model.ScenarioModel;
import org.perfcake.model.ScenarioModel.Generator;
import org.perfcake.model.ScenarioModel.Messages;
import org.perfcake.model.ScenarioModel.Messages.Message.ValidatorRef;
import org.perfcake.model.ScenarioModel.Reporting;
import org.perfcake.model.ScenarioModel.Sender;
import org.perfcake.model.ScenarioModel.Validation;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.ObjectFactory;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidatorManager;

/**
 * 
 * @author Jiri Sedlacek <jiri@sedlackovi.cz>
 * 
 */
public class ScenarioFactory {

   private static final String DEFAULT_GENERATOR_PACKAGE = "org.perfcake.message.generator";
   private static final String DEFAULT_SENDER_PACKAGE = "org.perfcake.message.sender";
   private static final String DEFAULT_REPORTER_PACKAGE = "org.perfcake.reporting.reporters";
   private static final String DEFAULT_DESTINATION_PACKAGE = "org.perfcake.reporting.destinations";
   private static final String DEFAULT_VALIDATION_PACKAGE = "org.perfcake.validation";

   public static final Logger log = Logger.getLogger(ScenarioFactory.class);
   
   private ScenarioModel scenario;

   public ScenarioFactory(ScenarioModel model) {
      this.scenario = model;
   }

   Scenario fromUrl(final URL scenario) {
      //ScenarioParser parser  = new ScenarioParser(scenario);
      return null;
   }
   
   /**
    * Parses RunInfo from generator configuration.
    * 
    * @return RunInfo object representing the configuration
    * @throws PerfCakeException
    *            when there is a parse exception
    */
   public RunInfo parseRunInfo() throws PerfCakeException {
      Generator gen = scenario.getGenerator();
      Generator.Run run = gen.getRun();
      RunInfo runInfo = new RunInfo(new Period(PeriodType.valueOf(run.getType().toUpperCase()), Long.valueOf(run.getValue())));
      
      return runInfo;
   }

   /**
    * Parse the <code>sender</code> element into a {@link AbstractMessageGenerator} instance.
    * 
    * @param scenarioNode
    *           DOM representation of the <code>performance</code> element of
    *           the scenario's definition.
    * @return A message generator.
    * @throws XPathExpressionException
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws ClassNotFoundException
    */
   public AbstractMessageGenerator parseGenerator() throws PerfCakeException {
      AbstractMessageGenerator generator = null;

      try {         
         Generator gen = scenario.getGenerator();
         String generatorClass = gen.getClazz();
         if (generatorClass.indexOf(".") < 0) {
            generatorClass = DEFAULT_GENERATOR_PACKAGE + "." + generatorClass;
         }
         log.info("--- Generator (" + generatorClass + ") ---");

         int threads = Integer.valueOf(gen.getThreads());
         log.info("  threads=" + threads);

         Properties generatorProperties = getPropertiesFromList(gen.getProperties());
         Utils.logProperties(log, Level.DEBUG, generatorProperties, "   ");

         generator = (AbstractMessageGenerator) ObjectFactory.summonInstance(generatorClass, generatorProperties);
         generator.setThreads(threads);
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
         throw new PerfCakeException("Cannot parse message generator configuration: ", e);
      }

      return generator;
   }

   /**
    * Parse the <code>sender</code> element into a {@link MessageSenderManager} instance.
    * 
    * @param senderPoolSize
    *           Size of the message sender pool.
    * @param scenarioNode
    *           DOM representation of the <code>performance</code> element of
    *           the scenario's definition.
    * @return A message sender manager.
    * @throws XPathExpressionException
    */
   public MessageSenderManager parseSender(final int senderPoolSize) throws PerfCakeException {
      MessageSenderManager msm;

      Sender sen = scenario.getSender();
      String senderClass = sen.getClazz();
      if (senderClass.indexOf(".") < 0) {
         senderClass = DEFAULT_SENDER_PACKAGE + "." + senderClass;
      }
      log.info("--- Sender (" + senderClass + ") ---");

      Properties senderProperties = getPropertiesFromList(sen.getProperties());
      Utils.logProperties(log, Level.DEBUG, senderProperties, "   ");

      msm = new MessageSenderManager();
      msm.setSenderClass(senderClass);
      msm.setSenderPoolSize(senderPoolSize);
      for (Entry<Object, Object> sProperty : senderProperties.entrySet()) {
         msm.setMessageSenderProperty(sProperty.getKey(),
               sProperty.getValue());
      }
      return msm;
   }

   /**
    * Parse the <code>messages</code> element into a message store.
    * 
    * @param scenarioNode
    *           DOM representation of the <code>performance</code> element of
    *           the scenario's definition.
    * @return Message store in a form of {@link Map}&lt;{@link Message}, {@link Long}&gt; where the keys are stored messages and the values
    *         are multiplicity of how many times the message is sent in a single
    *         iteration.
    * @throws XPathExpressionException
    * @throws IOException
    * @throws FileNotFoundException
    */
   public List<MessageTemplate> parseMessages() throws PerfCakeException {
      List<MessageTemplate> messageStore = new ArrayList<>();
      
      try {
         Messages messages = scenario.getMessages();
         if (messages != null) {
            
            log.info("--- Messages ---");
            // File messagesDir = new File(Utils.getProperty("perfcake.messages.dir", Utils.resourcesDir.getAbsolutePath() + "/messages"));
            for (Messages.Message m : messages.getMessages()) {
               
               URL messageUrl = Utils.locationToUrl(m.getUri(), "perfcake.messages.dir", Utils.determineDefaultLocation("messages"), "");
               String currentMessagePayload = Utils.readFilteredContent(messageUrl);
               Properties currentMessageProperties = getPropertiesFromList(m.getProperties());
               Properties currentMessageHeaders = new Properties();
               for (Header h : m.getHeaders()) {
                  currentMessageHeaders.setProperty(h.getName(), h.getValue());
               }

               Message currentMessage = new Message(currentMessagePayload);
               currentMessage.setProperties(currentMessageProperties);
               currentMessage.setHeaders(currentMessageHeaders);

               long currentMessageMultiplicity = -1;
               if (m.getMultiplicity() == null || m.getMultiplicity().equals("")) {
                  currentMessageMultiplicity  = 1L;
               } else {
                  currentMessageMultiplicity = Long.valueOf(m.getMultiplicity());
               }
               
               List<String> currentMessageValidatorRefList = new LinkedList<>();
               for (ValidatorRef ref : m.getValidatorRefs()) {
                  currentMessageValidatorRefList.add(ref.getId());
               }

               // create message to be send
               MessageTemplate currentMessageToSend = new MessageTemplate(currentMessage, currentMessageMultiplicity, currentMessageValidatorRefList);

               log.info("'- Message (" + messageUrl.toString() + "), " + currentMessageMultiplicity + "x");
               if (log.isDebugEnabled()) {
                  log.debug("  '- Properties:");
                  Utils.logProperties(log, Level.DEBUG, currentMessageProperties, "   '- ");
                  log.debug("  '- Headers:");
                  Utils.logProperties(log, Level.DEBUG, currentMessageHeaders, "   '- ");
               }

               messageStore.add(currentMessageToSend);
            }
         }
      } catch (IOException e) {
         throw new PerfCakeException("Cannot read messages content: ", e);
      }
      return messageStore;
   }

   /**
    * Parse the <code>reporting</code> element into a {@link ReportManager} instance.
    * 
    * @param scenarioNode
    *           DOM representation of the <code>performance</code> element of
    *           the scenario's definition.
    * @return Report manager.
    * @throws XPathExpressionException
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws ClassNotFoundException
    */
   public ReportManager parseReporting() throws PerfCakeException {
      ReportManager reportManager = new ReportManager();

      try {
         log.info("--- Reporting ---");
         Reporting reporting = scenario.getReporting();
         Properties reportingProperties = getPropertiesFromList(reporting.getProperties());
         Utils.logProperties(log, Level.DEBUG, reportingProperties, "   ");

         ObjectFactory.setPropertiesOnObject(reportManager, reportingProperties);

         for (Reporting.Reporter r : reporting.getReporters()) {
            
            Properties currentReporterProperties = getPropertiesFromList(r.getProperties());
            String reportClass = r.getClazz();
            if (reportClass.indexOf(".") < 0) {
               reportClass = DEFAULT_REPORTER_PACKAGE + "." + reportClass;
            }
            Reporter currentReporter = (Reporter) ObjectFactory.summonInstance(reportClass, currentReporterProperties);

            log.info("'- Reporter (" + reportClass + ")");
            
            for (Reporting.Reporter.Destination d : r.getDestinations()) {
               
               String destClass = d.getClazz();
               if (destClass.indexOf(".") < 0) {
                  destClass = DEFAULT_DESTINATION_PACKAGE + "." + destClass;
               }
               log.info(" '- Destination (" + destClass + ")");
               Properties currentDestinationProperties = getPropertiesFromList(d.getProperties());
               Utils.logProperties(log, Level.DEBUG, currentDestinationProperties, "  '- ");
               
               Destination currentDestination = (Destination) ObjectFactory.summonInstance(destClass, currentDestinationProperties);
               Set<Period> currentDestinationPeriodSet = new HashSet<>();
               for (org.perfcake.model.ScenarioModel.Reporting.Reporter.Destination.Period p : d.getPeriods()) {
                  currentDestinationPeriodSet.add(new Period(PeriodType.valueOf(p.getType().toUpperCase()), Long.valueOf(p.getValue())));
               }
               currentReporter.registerDestination(currentDestination, currentDestinationPeriodSet );
            }
            reportManager.registerReporter(currentReporter);
         }
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
         throw new PerfCakeException("Cannot parse reporting configuration: ", e);
      }

      return reportManager;
   }

   public void parseValidation() throws PerfCakeException {
      log.info("--- Validation ---");
      try {

         Validation validation = scenario.getValidation();
         if (validation != null) {
            
            for (Validation.Validator v : validation.getValidators()) {
               
               
               String validatorClass = v.getClazz();
               if (validatorClass.indexOf(".") < 0) {
                  validatorClass = DEFAULT_VALIDATION_PACKAGE + "." + validatorClass;
               }

               MessageValidator messageValidator = (MessageValidator) Class.forName(validatorClass, false, ScenarioExecution.class.getClassLoader()).newInstance();
               //TODO messageValidator.setAssertions(validatorNodes.item(i), "1");// add validator to validator mgr coll

               ValidatorManager.addValidator(v.getId(), messageValidator);
               ValidatorManager.setEnabled(true);
            }
         }
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
         throw new PerfCakeException("Cannot parse validation configuration: ", e);
      }
   }

   public Properties parseScenarioProperties() throws PerfCakeException {
      log.info("--- Scenario properties ---");
      return getPropertiesFromList(scenario.getProperties().getProperties());
   }
   
   private static Properties getPropertiesFromList(List<Property> properties) {
      Properties props = new Properties();
      for (Property p : properties) {
         props.setProperty(p.getName(), p.getValue());
      }
      return props;
   }
}
