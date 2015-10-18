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
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.message.sequence.Sequence;
import org.perfcake.message.sequence.SequenceManager;
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * Loads the scenario from an XML file.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class XmlFactory implements ScenarioFactory {

   /**
    * A logger.
    */
   private static final Logger log = LogManager.getLogger(XmlFactory.class);

   /**
    * DOM model of the scenario.
    */
   private org.perfcake.model.Scenario scenarioModel;

   /**
    * The scenario definition loaded from the file.
    */
   private String scenarioConfig;

   /**
    * Parsed scenario object.
    */
   private Scenario scenario = null;

   /**
    * The factory is not part of public API.
    */
   protected XmlFactory() {

   }

   @Override
   public void init(final URL scenarioURL) throws PerfCakeException {
      try {
         prepareModelTwoPass(scenarioURL);

         if (log.isDebugEnabled()) {
            log.debug(String.format("Loaded scenario definition from '%s'.", scenarioURL.toString()));
         }
      } catch (final IOException e) {
         throw new PerfCakeException("Cannot read scenario configuration: ", e);
      }
   }

   /**
    * Parses the scenario twice, first to read the properties defined in it, second using the new properties directly
    * in the scenario.
    *
    * @param scenarioURL
    *       Scenario location URL.
    * @throws PerfCakeException
    *       When it was not possible to parse the scenario.
    * @throws IOException
    *       When it was not possible to read the scenario definition.
    */
   private void prepareModelTwoPass(final URL scenarioURL) throws PerfCakeException, IOException {
      // two-pass parsing to first read the properties specified in the scenario and then use them
      this.scenarioConfig = Utils.readFilteredContent(scenarioURL);
      this.scenarioModel = parse();
      putScenarioPropertiesToSystem(parseScenarioProperties());

      this.scenarioConfig = Utils.readFilteredContent(scenarioURL);
      this.scenarioModel = parse();
      final Properties scenarioProperties = parseScenarioProperties();
      putScenarioPropertiesToSystem(scenarioProperties);

      if (log.isDebugEnabled() && scenarioProperties != null) {
         log.debug("--- Scenario Properties ---");
         scenarioProperties.forEach((key, value) -> log.debug("'- {}:{}", key, value));
      }
   }

   @Override
   public synchronized Scenario getScenario() throws PerfCakeException {
      if (scenario == null) {
         scenario = new Scenario();

         final RunInfo runInfo = parseRunInfo();
         final MessageGenerator messageGenerator = parseGenerator();
         messageGenerator.setRunInfo(runInfo);

         scenario.setGenerator(messageGenerator);
         scenario.setMessageSenderManager(parseSender(messageGenerator.getThreads()));
         scenario.setReportManager(parseReporting());
         scenario.getReportManager().setRunInfo(runInfo);

         final ValidationManager validationManager = parseValidation();
         final List<MessageTemplate> messageTemplates = parseMessages(validationManager);
         scenario.setMessageStore(messageTemplates);
         scenario.setValidationManager(validationManager);
         scenario.setSequenceManager(parseSequences());
      }

      return scenario;
   }

   /**
    * Does the parsing itself by using JAXB.
    *
    * @return Parsed JAXB scenario model.
    * @throws PerfCakeException
    *       If XML is not valid or cannot be successfully parsed.
    */
   private org.perfcake.model.Scenario parse() throws PerfCakeException {
      try {
         final Source scenarioXML = new StreamSource(new ByteArrayInputStream(scenarioConfig.getBytes(Utils.getDefaultEncoding())));
         final String schemaFileName = "perfcake-scenario-" + PerfCakeConst.XSD_SCHEMA_VERSION + ".xsd";
         final URL backupUrl = new URL("http://schema.perfcake.org/" + schemaFileName);

         URL scenarioXsdUrl = Utils.getResourceAsUrl("/schemas/" + schemaFileName);

         try {
            InputStream test = scenarioXsdUrl.openStream();
            //noinspection ResultOfMethodCallIgnored
            test.read(); // there always is a byte
            test.close(); // we do not need finally for this as we could not have failed
         } catch (IOException e) {
            scenarioXsdUrl = backupUrl; // backup taken from the web
         }

         final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         final Schema schema = schemaFactory.newSchema(scenarioXsdUrl);

         final JAXBContext context = JAXBContext.newInstance(org.perfcake.model.Scenario.class);
         final Unmarshaller unmarshaller = context.createUnmarshaller();
         unmarshaller.setSchema(schema);
         return (org.perfcake.model.Scenario) unmarshaller.unmarshal(scenarioXML);
      } catch (final SAXException e) {
         throw new PerfCakeException("Cannot validate scenario configuration. PerfCake installation seems broken. ", e);
      } catch (final JAXBException e) {
         throw new PerfCakeException("Cannot parse scenario configuration: ", e);
      } catch (final MalformedURLException e) {
         throw new PerfCakeException("Cannot read scenario schema to validate it: ", e);
      } catch (final UnsupportedEncodingException e) {
         throw new PerfCakeException("set encoding is not supported: ", e);
      }
   }

   private void putScenarioPropertiesToSystem(final Properties properties) {
      if (properties != null) {
         properties.forEach(System.getProperties()::put);
      }
   }

   private static Properties getPropertiesFromList(final List<Property> properties) throws PerfCakeException {
      final Properties props = new Properties();

      for (final Property p : properties) {
         final Element valueElement = p.getAny();
         final String valueString = p.getValue();

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
    * Parses {@link org.perfcake.RunInfo} from the generator configuration.
    *
    * @return {@link org.perfcake.RunInfo} representing the configuration.
    * @throws PerfCakeException
    *       When there is a parse exception.
    */
   protected RunInfo parseRunInfo() throws PerfCakeException {
      final org.perfcake.model.Scenario.Run run = scenarioModel.getRun();
      final RunInfo runInfo = new RunInfo(new Period(PeriodType.valueOf(run.getType().toUpperCase()), Long.parseLong(run.getValue())));

      if (log.isDebugEnabled()) {
         log.debug("--- Run Info ---");
         log.debug("  " + runInfo.toString());
      }

      return runInfo;
   }

   /**
    * Parses the <code>generator</code> element into an {@link org.perfcake.message.generator.MessageGenerator} instance.
    *
    * @return A particular implementation of {@link org.perfcake.message.generator.MessageGenerator}.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected MessageGenerator parseGenerator() throws PerfCakeException {
      final MessageGenerator generator;

      try {
         final Generator gen = scenarioModel.getGenerator();
         String generatorClass = gen.getClazz();
         if (!generatorClass.contains(".")) {
            generatorClass = DEFAULT_GENERATOR_PACKAGE + "." + generatorClass;
         }

         final int threads = Integer.parseInt(gen.getThreads());

         if (log.isDebugEnabled()) {
            log.debug("--- Generator (" + generatorClass + ") ---");
            log.debug("  threads=" + threads);
         }

         final Properties generatorProperties = getPropertiesFromList(gen.getProperty());
         Utils.logProperties(log, Level.DEBUG, generatorProperties, "   ");

         generator = (MessageGenerator) ObjectFactory.summonInstance(generatorClass, generatorProperties);
         generator.setThreads(threads);
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
         throw new PerfCakeException("Cannot parse message generator configuration: ", e);
      }

      return generator;
   }

   /**
    * Parses the <code>sequences</code> element into a {@link SequenceManager} instance.
    *
    * @return {@link SequenceManager} containing the parsed {@link Sequence Sequences}.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected SequenceManager parseSequences() throws PerfCakeException {
      final SequenceManager sequenceManager = new SequenceManager();

      try {
         final org.perfcake.model.Scenario.Sequences sequences = scenarioModel.getSequences();

         if (sequences != null) {
            for (org.perfcake.model.Scenario.Sequences.Sequence seq : sequences.getSequence()) {
               final String sequenceName = seq.getName();
               String sequenceClass = seq.getClazz();
               final Properties sequenceProperties = getPropertiesFromList(seq.getProperty());

               if (!sequenceClass.contains(".")) {
                  sequenceClass = DEFAULT_SEQUENCE_PACKAGE + "." + sequenceClass;
               }

               if (log.isDebugEnabled()) {
                  log.debug("--- Sequence (" + sequenceName + ":" + sequenceClass + ") ---");
               }

               Utils.logProperties(log, Level.DEBUG, sequenceProperties, "   ");

               final Sequence sequence = (Sequence) ObjectFactory.summonInstance(sequenceClass, sequenceProperties);
               sequenceManager.addSequence(sequenceName, sequence);
            }
         }
      } catch (ReflectiveOperationException e) {
         throw new PerfCakeException("Cannot parse sequences: ", e);
      }

      return sequenceManager;
   }

   /**
    * Parses the <code>sender</code> element into a {@link org.perfcake.message.sender.MessageSenderManager} instance.
    *
    * @param senderPoolSize
    *       Size of the message sender pool.
    * @return {@link org.perfcake.message.sender.MessageSenderManager} containing the parsed {@link org.perfcake.message.sender.MessageSender}s.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected MessageSenderManager parseSender(final int senderPoolSize) throws PerfCakeException {
      final MessageSenderManager msm;

      final Sender sen = scenarioModel.getSender();
      String senderClass = sen.getClazz();
      if (!senderClass.contains(".")) {
         senderClass = DEFAULT_SENDER_PACKAGE + "." + senderClass;
      }

      if (log.isDebugEnabled()) {
         log.debug("--- Sender (" + senderClass + ") ---");
      }

      final Properties senderProperties = getPropertiesFromList(sen.getProperty());
      Utils.logProperties(log, Level.DEBUG, senderProperties, "   ");

      msm = new MessageSenderManager();
      msm.setSenderClass(senderClass);
      msm.setSenderPoolSize(senderPoolSize);
      if (sen.getTarget() != null) {
         msm.setMessageSenderProperty("target", sen.getTarget());
      }
      for (final Entry<Object, Object> sProperty : senderProperties.entrySet()) {
         msm.setMessageSenderProperty(sProperty.getKey(), sProperty.getValue());
      }
      return msm;
   }

   /**
    * Parses the <code>messages</code> element into a list of {@link org.perfcake.message.MessageTemplate}s.
    *
    * @param validationManager
    *       {@link org.perfcake.validation.ValidationManager} carrying all parsed validators, these will be associated with the message templates.
    * @return A list of {@link org.perfcake.message.MessageTemplate}s.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected List<MessageTemplate> parseMessages(final ValidationManager validationManager) throws PerfCakeException {
      final List<MessageTemplate> messageStore = new ArrayList<>();

      try {
         final Messages messages = scenarioModel.getMessages();
         if (messages != null) {

            if (log.isDebugEnabled()) {
               log.debug("--- Messages ---");
            }
            for (final Messages.Message m : messages.getMessage()) {
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

               final Properties currentMessageProperties = getPropertiesFromList(m.getProperty());
               final Properties currentMessageHeaders = new Properties();
               for (final Header h : m.getHeader()) {
                  currentMessageHeaders.setProperty(h.getName(), h.getValue());
               }

               final Message currentMessage = new Message(currentMessagePayload);
               currentMessage.setProperties(currentMessageProperties);
               currentMessage.setHeaders(currentMessageHeaders);

               long currentMessageMultiplicity;
               if (m.getMultiplicity() == null || m.getMultiplicity().equals("")) {
                  currentMessageMultiplicity = 1L;
               } else {
                  currentMessageMultiplicity = Long.parseLong(m.getMultiplicity());
               }

               final List<String> currentMessageValidatorIds = new ArrayList<>();
               for (final ValidatorRef ref : m.getValidatorRef()) {
                  final MessageValidator validator = validationManager.getValidator(ref.getId());
                  if (validator == null) {
                     throw new PerfCakeException(String.format("Validator with id %s not found.", ref.getId()));
                  }

                  currentMessageValidatorIds.add(ref.getId());
               }

               // create message to be send
               final MessageTemplate currentMessageToSend = new MessageTemplate(currentMessage, currentMessageMultiplicity, currentMessageValidatorIds);

               if (log.isDebugEnabled()) {
                  log.debug("'- Message (" + (messageUrl != null ? messageUrl.toString() : "") + "), " + currentMessageMultiplicity + "x");
                  log.debug("  '- Properties:");
                  Utils.logProperties(log, Level.DEBUG, currentMessageProperties, "   '- ");
                  log.debug("  '- Headers:");
                  Utils.logProperties(log, Level.DEBUG, currentMessageHeaders, "   '- ");
               }

               messageStore.add(currentMessageToSend);
            }
         }
      } catch (final IOException e) {
         throw new PerfCakeException("Cannot read messages content: ", e);
      }
      return messageStore;
   }

   /**
    * Parse the <code>reporting</code> element into a {@link org.perfcake.reporting.ReportManager} instance.
    *
    * @return Parsed {@link org.perfcake.reporting.ReportManager}.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected ReportManager parseReporting() throws PerfCakeException {
      final ReportManager reportManager = new ReportManager();

      try {
         if (log.isDebugEnabled()) {
            log.debug("--- Reporting ---");
         }
         final Reporting reporting = scenarioModel.getReporting();
         if (reporting != null) {
            final Properties reportingProperties = getPropertiesFromList(reporting.getProperty());
            Utils.logProperties(log, Level.DEBUG, reportingProperties, "   ");

            ObjectFactory.setPropertiesOnObject(reportManager, reportingProperties);

            for (final Reporting.Reporter r : reporting.getReporter()) {
               if (r.isEnabled()) {
                  final Properties currentReporterProperties = getPropertiesFromList(r.getProperty());
                  String reportClass = r.getClazz();
                  if (!reportClass.contains(".")) {
                     reportClass = DEFAULT_REPORTER_PACKAGE + "." + reportClass;
                  }
                  final Reporter currentReporter = (Reporter) ObjectFactory.summonInstance(reportClass, currentReporterProperties);

                  if (log.isDebugEnabled()) {
                     log.debug("'- Reporter (" + reportClass + ")");
                  }

                  for (final Reporting.Reporter.Destination d : r.getDestination()) {
                     if (d.isEnabled()) {
                        String destClass = d.getClazz();
                        if (!destClass.contains(".")) {
                           destClass = DEFAULT_DESTINATION_PACKAGE + "." + destClass;
                        }

                        if (log.isDebugEnabled()) {
                           log.debug(" '- Destination (" + destClass + ")");
                        }
                        final Properties currentDestinationProperties = getPropertiesFromList(d.getProperty());
                        Utils.logProperties(log, Level.DEBUG, currentDestinationProperties, "  '- ");

                        final Destination currentDestination = (Destination) ObjectFactory.summonInstance(destClass, currentDestinationProperties);
                        final Set<Period> currentDestinationPeriodSet = new HashSet<>();
                        for (final org.perfcake.model.Scenario.Reporting.Reporter.Destination.Period p : d.getPeriod()) {
                           currentDestinationPeriodSet.add(new Period(PeriodType.valueOf(p.getType().toUpperCase()), Long.parseLong(p.getValue())));
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

   /**
    * Parse the <code>validation</code> element into a {@link org.perfcake.validation.ValidationManager} instance.
    *
    * @return Parsed {@link org.perfcake.validation.ValidationManager}.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected ValidationManager parseValidation() throws PerfCakeException {
      final ValidationManager validationManager = new ValidationManager();

      if (log.isDebugEnabled()) {
         log.debug("--- Validation ---");
      }
      try {

         final Validation validation = scenarioModel.getValidation();
         if (validation != null) {

            for (final Validation.Validator v : validation.getValidator()) {

               String validatorClass = v.getClazz();
               if (!validatorClass.contains(".")) {
                  validatorClass = DEFAULT_VALIDATION_PACKAGE + "." + validatorClass;
               }

               if (log.isDebugEnabled()) {
                  log.debug(" '- Validation (" + v.getId() + ":" + validatorClass + ")");
               }
               final Properties currentValidationProperties = getPropertiesFromList(v.getProperty());
               Utils.logProperties(log, Level.DEBUG, currentValidationProperties, "  '- ");

               final MessageValidator messageValidator = (MessageValidator) ObjectFactory.summonInstance(validatorClass, currentValidationProperties);

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

   /**
    * Parse the <code>properties</code> element into a {@link java.util.Properties} instance.
    *
    * @return Parsed properties.
    * @throws org.perfcake.PerfCakeException
    *       When there is a parse exception.
    */
   protected Properties parseScenarioProperties() throws PerfCakeException {
      if (scenarioModel.getProperties() != null) {
         if (scenarioModel.getProperties().getProperty() != null) {
            return getPropertiesFromList(scenarioModel.getProperties().getProperty());
         }
      }

      return null;
   }
}
