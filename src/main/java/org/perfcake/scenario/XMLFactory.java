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
package org.perfcake.scenario;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.model.Header;
import org.perfcake.model.Property;
import org.perfcake.model.Scenario.Generator;
import org.perfcake.model.Scenario.Messages;
import org.perfcake.model.Scenario.Messages.Message.ValidatorRef;
import org.perfcake.model.Scenario.Reporting;
import org.perfcake.model.Scenario.Sender;
import org.perfcake.model.Scenario.Validation;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.ObjectFactory;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidationManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * TODO review logging
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class XMLFactory implements ScenarioFactory {

   public static final Logger log = Logger.getLogger(XMLFactory.class);
   private org.perfcake.model.Scenario scenarioModel;
   private String scenarioConfig;
   private Scenario scenario = null;

   protected XMLFactory() {

   }

   public void init(final URL scenarioURL) throws PerfCakeException {
      try {
         this.scenarioConfig = Utils.readFilteredContent(scenarioURL);

      } catch (IOException e) {
         throw new PerfCakeException("Cannot read scenario configuration: ", e);
      }

      this.scenarioModel = parse();
   }

   public synchronized Scenario getScenario() throws PerfCakeException {
      if (scenario == null) {
         scenario = new Scenario();

         RunInfo runInfo = parseRunInfo();
         AbstractMessageGenerator messageGenerator = parseGenerator();
         messageGenerator.setRunInfo(runInfo);

         scenario.setGenerator(messageGenerator);
         scenario.setMessageSenderManager(parseSender(messageGenerator.getThreads()));
         scenario.setReportManager(parseReporting());
         scenario.getReportManager().setRunInfo(runInfo);

         ValidationManager validationManager = parseValidation();
         List<MessageTemplate> messageTemplates = parseMessages(validationManager);
         scenario.setMessageStore(messageTemplates);
         scenario.setValidationManager(validationManager);
      }

      return scenario;
   }

   /**
    * Do the parsing itself by using JAXB
    *
    * @return parsed JAXB scenario model
    * @throws PerfCakeException
    *       if XML is not valid or cannot be successfully parsed
    */
   private org.perfcake.model.Scenario parse() throws PerfCakeException {
      try {
         Source scenarioXML = new StreamSource(new ByteArrayInputStream(scenarioConfig.getBytes(Utils.getDefaultEncoding())));
         String schemaFileName = "perfcake-scenario-" + PerfCakeConst.XSD_SCHEMA_VERSION + ".xsd";

         URL scenarioXsdUrl = getClass().getResource("/schemas/" + schemaFileName);
         if (scenarioXsdUrl == null) { // backup taken from web
            scenarioXsdUrl = new URL("http://schema.perfcake.org/" + schemaFileName);
         }

         SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         Schema schema = schemaFactory.newSchema(scenarioXsdUrl);

         JAXBContext context = JAXBContext.newInstance(org.perfcake.model.Scenario.class);
         Unmarshaller unmarshaller = context.createUnmarshaller();
         unmarshaller.setSchema(schema);
         return (org.perfcake.model.Scenario) unmarshaller.unmarshal(scenarioXML);
      } catch (SAXException e) {
         throw new PerfCakeException("Cannot validate scenario configuration: ", e);
      } catch (JAXBException e) {
         throw new PerfCakeException("Cannot parse scenario configuration: ", e);
      } catch (MalformedURLException e) {
         throw new PerfCakeException("Cannot read scenario schema to validate it: ", e);
      } catch (UnsupportedEncodingException e) {
         throw new PerfCakeException("set encoding is not supported: ", e);
      }
   }

   private static Properties getPropertiesFromList(List<Property> properties) throws PerfCakeException {
      Properties props = new Properties();

      for (Property p : properties) {
         Element valueElement = p.getAny();
         String valueString = p.getValue();

         if (valueElement != null && valueString != null) {
            throw new PerfCakeException(String.format("A property tag can either have an attribute value (%s) or the body (%s) set, not both at the same time.", valueString, valueElement.toString()));
         } else if (valueElement == null && valueString == null) {
            throw new PerfCakeException("A property tag must either have an attribute value or the body set.");
         }

         props.put(p.getName(), valueString == null ? valueElement : valueString);
      }

      return props;
   }

   /**
    * Parses RunInfo from generator configuration.
    *
    * @return RunInfo object representing the configuration
    * @throws PerfCakeException
    *       when there is a parse exception
    */
   protected RunInfo parseRunInfo() throws PerfCakeException {
      Generator gen = scenarioModel.getGenerator();
      Generator.Run run = gen.getRun();

      return new RunInfo(new Period(PeriodType.valueOf(run.getType().toUpperCase()), Long.valueOf(run.getValue())));
   }

   /**
    * Parse the <code>generator</code> element into a {@link AbstractMessageGenerator} instance.
    *
    * @return A message generator.
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws ClassNotFoundException
    */
   protected AbstractMessageGenerator parseGenerator() throws PerfCakeException {
      AbstractMessageGenerator generator = null;

      try {
         Generator gen = scenarioModel.getGenerator();
         String generatorClass = gen.getClazz();
         if (!generatorClass.contains(".")) {
            generatorClass = DEFAULT_GENERATOR_PACKAGE + "." + generatorClass;
         }
         log.info("--- Generator (" + generatorClass + ") ---");

         int threads = Integer.valueOf(gen.getThreads());
         log.info("  threads=" + threads);

         Properties generatorProperties = getPropertiesFromList(gen.getProperty());
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
    *       Size of the message sender pool.
    * @return A message sender manager.
    */
   protected MessageSenderManager parseSender(final int senderPoolSize) throws PerfCakeException {
      MessageSenderManager msm;

      Sender sen = scenarioModel.getSender();
      String senderClass = sen.getClazz();
      if (!senderClass.contains(".")) {
         senderClass = DEFAULT_SENDER_PACKAGE + "." + senderClass;
      }
      log.info("--- Sender (" + senderClass + ") ---");

      Properties senderProperties = getPropertiesFromList(sen.getProperty());
      Utils.logProperties(log, Level.DEBUG, senderProperties, "   ");

      msm = new MessageSenderManager();
      msm.setSenderClass(senderClass);
      msm.setSenderPoolSize(senderPoolSize);
      for (Entry<Object, Object> sProperty : senderProperties.entrySet()) {
         msm.setMessageSenderProperty(sProperty.getKey(), sProperty.getValue());
      }
      return msm;
   }

   /**
    * Parse the <code>messages</code> element into a message store.
    *
    * @param validationManager
    *       ValidationManager carrying all parsed validators, these will be associated with the message templates.
    * @return Message store in a form of {@link Map}&lt;{@link Message}, {@link Long}&gt; where the keys are stored messages and the values
    * are multiplicity of how many times the message is sent in a single
    * iteration.
    * @throws IOException
    * @throws FileNotFoundException
    */
   protected List<MessageTemplate> parseMessages(ValidationManager validationManager) throws PerfCakeException {
      List<MessageTemplate> messageStore = new ArrayList<>();

      try {
         Messages messages = scenarioModel.getMessages();
         if (messages != null) {

            log.info("--- Messages ---");
            for (Messages.Message m : messages.getMessage()) {
               URL messageUrl = null;
               String currentMessagePayload;
               if (m.getContent() != null) {
                  if (m.getUri() != null) {
                     log.warn("Both 'content' and 'uri' attributes of a message element are set. 'uri' will be will be ignored");
                  }
                  currentMessagePayload = m.getContent();
               } else {
                  if (m.getUri() != null) {
                     messageUrl = Utils.locationToUrl(m.getUri(), PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.determineDefaultLocation("messages"), "");
                     currentMessagePayload = Utils.readFilteredContent(messageUrl);
                  } else {
                     messageUrl = null;
                     currentMessagePayload = null;
                  }
               }
               Properties currentMessageProperties = getPropertiesFromList(m.getProperty());
               Properties currentMessageHeaders = new Properties();
               for (Header h : m.getHeader()) {
                  currentMessageHeaders.setProperty(h.getName(), h.getValue());
               }

               Message currentMessage = new Message(currentMessagePayload);
               currentMessage.setProperties(currentMessageProperties);
               currentMessage.setHeaders(currentMessageHeaders);

               long currentMessageMultiplicity = -1;
               if (m.getMultiplicity() == null || m.getMultiplicity().equals("")) {
                  currentMessageMultiplicity = 1L;
               } else {
                  currentMessageMultiplicity = Long.valueOf(m.getMultiplicity());
               }

               final List<String> currentMessageValidatorIds = new ArrayList<>();
               for (ValidatorRef ref : m.getValidatorRef()) {
                  MessageValidator validator = validationManager.getValidator(ref.getId());
                  if (validator == null) {
                     throw new PerfCakeException(String.format("Validator with id %s not found.", ref.getId()));
                  }

                  currentMessageValidatorIds.add(ref.getId());
               }

               // create message to be send
               MessageTemplate currentMessageToSend = new MessageTemplate(currentMessage, currentMessageMultiplicity, currentMessageValidatorIds);

               log.info("'- Message (" + (messageUrl != null ? messageUrl.toString() : "") + "), " + currentMessageMultiplicity + "x");
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
    * @return Report manager.
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws ClassNotFoundException
    */
   protected ReportManager parseReporting() throws PerfCakeException {
      ReportManager reportManager = new ReportManager();

      try {
         log.info("--- Reporting ---");
         Reporting reporting = scenarioModel.getReporting();
         if (reporting != null) {
            Properties reportingProperties = getPropertiesFromList(reporting.getProperty());
            Utils.logProperties(log, Level.DEBUG, reportingProperties, "   ");

            ObjectFactory.setPropertiesOnObject(reportManager, reportingProperties);

            for (Reporting.Reporter r : reporting.getReporter()) {
               if (r.isEnabled()) {
                  Properties currentReporterProperties = getPropertiesFromList(r.getProperty());
                  String reportClass = r.getClazz();
                  if (!reportClass.contains(".")) {
                     reportClass = DEFAULT_REPORTER_PACKAGE + "." + reportClass;
                  }
                  Reporter currentReporter = (Reporter) ObjectFactory.summonInstance(reportClass, currentReporterProperties);

                  log.info("'- Reporter (" + reportClass + ")");

                  for (Reporting.Reporter.Destination d : r.getDestination()) {
                     if (d.isEnabled()) {
                        String destClass = d.getClazz();
                        if (!destClass.contains(".")) {
                           destClass = DEFAULT_DESTINATION_PACKAGE + "." + destClass;
                        }
                        log.info(" '- Destination (" + destClass + ")");
                        Properties currentDestinationProperties = getPropertiesFromList(d.getProperty());
                        Utils.logProperties(log, Level.DEBUG, currentDestinationProperties, "  '- ");

                        Destination currentDestination = (Destination) ObjectFactory.summonInstance(destClass, currentDestinationProperties);
                        Set<Period> currentDestinationPeriodSet = new HashSet<>();
                        for (org.perfcake.model.Scenario.Reporting.Reporter.Destination.Period p : d.getPeriod()) {
                           currentDestinationPeriodSet.add(new Period(PeriodType.valueOf(p.getType().toUpperCase()), Long.valueOf(p.getValue())));
                        }
                        currentReporter.registerDestination(currentDestination, currentDestinationPeriodSet);
                     }
                  }
                  reportManager.registerReporter(currentReporter);
               }
            }
         }
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
         throw new PerfCakeException("Cannot parse reporting configuration: ", e);
      }

      return reportManager;
   }

   protected ValidationManager parseValidation() throws PerfCakeException {
      final ValidationManager validationManager = new ValidationManager();

      log.info("--- Validation ---");
      try {

         Validation validation = scenarioModel.getValidation();
         if (validation != null) {

            for (Validation.Validator v : validation.getValidator()) {

               String validatorClass = v.getClazz();
               if (!validatorClass.contains(".")) {
                  validatorClass = DEFAULT_VALIDATION_PACKAGE + "." + validatorClass;
               }

               log.info(" '- Validation (" + validatorClass + ")");
               Properties currentValidationProperties = getPropertiesFromList(v.getProperty());
               Utils.logProperties(log, Level.DEBUG, currentValidationProperties, "  '- ");

               MessageValidator messageValidator = (MessageValidator) ObjectFactory.summonInstance(validatorClass, currentValidationProperties);

               validationManager.addValidator(v.getId(), messageValidator);
            }

            validationManager.setEnabled(validation.isEnabled());
            validationManager.setFastForward(validation.isFastForward());
         }
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
         throw new PerfCakeException("Cannot parse validation configuration: ", e);
      }

      return validationManager;
   }

   protected Properties parseScenarioProperties() throws PerfCakeException {
      log.info("--- Scenario properties ---");
      return getPropertiesFromList(scenarioModel.getProperties().getProperty());
   }
}
