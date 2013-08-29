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
package org.perfcake.parser;

import java.io.ByteArrayInputStream;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.RunInfo;
import org.perfcake.Scenario;
import org.perfcake.ScenarioExecution;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.message.Message;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.ObjectFactory;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidatorManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * TODO review logging and refactor parsing
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ScenarioParser {

   private static final String DEFAULT_GENERATOR_PACKAGE = "org.perfcake.message.generator";
   private static final String DEFAULT_SENDER_PACKAGE = "org.perfcake.message.sender";
   private static final String DEFAULT_REPORTER_PACKAGE = "org.perfcake.reporting.reporters";
   private static final String DEFAULT_DESTINATION_PACKAGE = "org.perfcake.reporting.destinations";
   private static final String DEFAULT_VALIDATION_PACKAGE = "org.perfcake.validation";

   public static final Logger log = Logger.getLogger(ScenarioExecution.class);

   private static final XPath xpath = XPathFactory.newInstance().newXPath();

   private String scenarioConfig;
   private final Node scenarioNode;

   public ScenarioParser(final URL scenario) throws PerfCakeException {
      try {
         this.scenarioConfig = Utils.readFilteredContent(scenario);

         final Source scenarioXML = new StreamSource(new ByteArrayInputStream(scenarioConfig.getBytes()));

         final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         final Schema schema = schemaFactory.newSchema(new URL("http://schema.perfcake.org/perfcake-scenario-" + Scenario.VERSION + ".xsd"));
         final Validator validator = schema.newValidator();

         if (log.isDebugEnabled()) {
            log.debug("Validating scenario configuration");
         }
         validator.validate(scenarioXML);
      } catch (final IOException e) {
         throw new PerfCakeException("Cannot read scenario configuration: ", e);
      } catch (final SAXException e) {
         throw new PerfCakeException("Cannot validate scenario configuration: ", e);
      }

      this.scenarioNode = parseScenarioNode();
   }

   /**
    * Parse the DOM node representation of the <code>performance</code> element
    * of the scenario's XML definition.
    * 
    * @param scenarioXmlString
    *           String containing the scenario's XML definition.
    * @return DOM representation of the <code>performance</code> element of the
    *         scenario's definition.
    * @throws PerfCakeException
    */
   private Node parseScenarioNode() throws PerfCakeException {
      try {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();
         return builder.parse(new ByteArrayInputStream(scenarioConfig.getBytes())).getDocumentElement();
      } catch (SAXException | IOException | ParserConfigurationException e) {
         throw new PerfCakeException("Cannot parse scenario configuration: ", e);
      }
   }

   /**
    * Parses RunInfo from generator configuration.
    * 
    * @return RunInfo object representing the configuration
    * @throws PerfCakeException
    *            when there is a parse exception
    */
   public RunInfo parseRunInfo() throws PerfCakeException {
      RunInfo runInfo = null;

      try {
         final Element generatorElement = (Element) (xPathEvaluate("generator", scenarioNode).item(0));
         final Element runInfoElement = (Element) (xPathEvaluate("run", generatorElement).item(0));
         runInfo = new RunInfo(new Period(PeriodType.valueOf(runInfoElement.getAttribute("type").toUpperCase()), Long.valueOf(runInfoElement.getAttribute("value"))));
      } catch (final XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse generator run information configuration: ", e);
      }

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
         final Element generatorElement = (Element) (xPathEvaluate("generator", scenarioNode).item(0));
         String generatorClass = generatorElement.getAttribute("class");
         if (generatorClass.indexOf(".") < 0) {
            generatorClass = DEFAULT_GENERATOR_PACKAGE + "." + generatorClass;
         }
         log.info("--- Generator (" + generatorClass + ") ---");

         final int threads = Integer.valueOf(generatorElement.getAttribute("threads"));
         log.info("  threads=" + threads);

         final Properties generatorProperties = getPropertiesFromSubNodes(generatorElement);
         Utils.logProperties(log, Level.DEBUG, generatorProperties, "   ");

         generator = (AbstractMessageGenerator) ObjectFactory.summonInstance(generatorClass, generatorProperties);
         generator.setThreads(threads);
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | XPathExpressionException e) {
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

      try {
         final Element senderElement = (Element) (xPathEvaluate("sender", scenarioNode).item(0));
         String senderClass = senderElement.getAttribute("class");
         if (senderClass.indexOf(".") < 0) {
            senderClass = DEFAULT_SENDER_PACKAGE + "." + senderClass;
         }
         log.info("--- Sender (" + senderClass + ") ---");

         final Properties senderProperties = getPropertiesFromSubNodes(senderElement);
         Utils.logProperties(log, Level.DEBUG, senderProperties, "   ");

         msm = new MessageSenderManager();
         msm.setSenderClass(senderClass);
         msm.setSenderPoolSize(senderPoolSize);
         for (final Entry<Object, Object> sProperty : senderProperties.entrySet()) {
            msm.setMessageSenderProperty(sProperty.getKey(), sProperty.getValue());
         }
      } catch (final XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse message sender manager configuration: ", e);
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
   public List<MessageTemplate> parseMessages(final ValidatorManager validatorManager) throws PerfCakeException {
      final List<MessageTemplate> messageStore = new ArrayList<>();

      try {
         final Element messagesElement = (Element) (xPathEvaluate("messages", scenarioNode)).item(0);
         if (messagesElement != null) {
            final NodeList messageNodes = xPathEvaluate("message", messagesElement);
            final int messageNodesCount = messageNodes.getLength();
            Message currentMessage = null;
            MessageTemplate currentMessageToSend = null;
            Element currentMessageElement = null;
            String currentMessagePayload = null;
            String currentMessageMultiplicityAttributeValue = null;
            Properties currentMessageProperties = null;
            Properties currentMessageHeaders = null;
            long currentMessageMultiplicity = -1;

            log.info("--- Messages ---");
            // File messagesDir = new File(Utils.getProperty("perfcake.messages.dir", Utils.resourcesDir.getAbsolutePath() + "/messages"));
            for (int messageNodeIndex = 0; messageNodeIndex < messageNodesCount; messageNodeIndex++) {
               currentMessageElement = (Element) messageNodes.item(messageNodeIndex);
               final URL messageUrl = Utils.locationToUrl(currentMessageElement.getAttribute("uri"), PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.determineDefaultLocation("messages"), "");
               currentMessagePayload = Utils.readFilteredContent(messageUrl);
               currentMessageProperties = getPropertiesFromSubNodes(currentMessageElement);
               currentMessageHeaders = getPropertiesFromSubNodes(currentMessageElement, "header", "name", "value");

               currentMessage = new Message(currentMessagePayload);
               currentMessage.setProperties(currentMessageProperties);
               currentMessage.setHeaders(currentMessageHeaders);

               currentMessageMultiplicityAttributeValue = currentMessageElement.getAttribute("multiplicity");
               if (currentMessageMultiplicityAttributeValue.equals("")) {
                  currentMessageMultiplicity = 1L;
               } else {
                  currentMessageMultiplicity = Long.valueOf(currentMessageMultiplicityAttributeValue);
               }

               final NodeList currentMessageValidatorRefNodeList = xPathEvaluate("validatorRef", currentMessageElement);
               final List<MessageValidator> currentMessageValidators = new LinkedList<>();
               Element currentMessageValidatorRefElement = null;
               final int validatorRefCount = currentMessageValidatorRefNodeList.getLength();

               for (int validatorRefIndex = 0; validatorRefIndex < validatorRefCount; validatorRefIndex++) {
                  String validatorId = null;
                  MessageValidator validator = null;

                  currentMessageValidatorRefElement = (Element) currentMessageValidatorRefNodeList.item(validatorRefIndex);

                  validatorId = currentMessageValidatorRefElement.getAttribute("id");
                  validator = validatorManager.getValidator(validatorId);
                  if (validator == null) {
                     throw new PerfCakeException(String.format("Validator with id %s not found.", validatorId));
                  }

                  currentMessageValidators.add(validator);
               }

               // create message to be send
               currentMessageToSend = new MessageTemplate(currentMessage, currentMessageMultiplicity, currentMessageValidators);

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
      } catch (final XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse messages configuration: ", e);
      } catch (final IOException e) {
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
      final ReportManager reportManager = new ReportManager();

      try {
         log.info("--- Reporting ---");
         final Element reportingElement = (Element) (xPathEvaluate("reporting", scenarioNode).item(0));
         final Properties reportingProperties = getPropertiesFromSubNodes(reportingElement);
         Utils.logProperties(log, Level.DEBUG, reportingProperties, "   ");

         ObjectFactory.setPropertiesOnObject(reportManager, reportingProperties);

         final NodeList reporterNodes = xPathEvaluate("reporter", reportingElement);
         final int reporterNodesCount = reporterNodes.getLength();
         Reporter currentReporter = null;
         Element currentReporterElement = null;
         Properties currentReporterProperties = null;
         NodeList currentReporterDestinations = null;

         for (int i = 0; i < reporterNodesCount; i++) {
            currentReporterElement = (Element) reporterNodes.item(i);
            final String reporterEnabled = currentReporterElement.getAttribute("enabled");

            if (reporterEnabled == null || "".equals(reporterEnabled) || Boolean.parseBoolean(reporterEnabled)) { // ignore disabled destinations
               currentReporterProperties = getPropertiesFromSubNodes(currentReporterElement);
               String reportClass = currentReporterElement.getAttribute("class");
               if (reportClass.indexOf(".") < 0) {
                  reportClass = DEFAULT_REPORTER_PACKAGE + "." + reportClass;
               }
               currentReporter = (Reporter) ObjectFactory.summonInstance(reportClass, currentReporterProperties);

               log.info("'- Reporter (" + reportClass + ")");
               currentReporterDestinations = xPathEvaluate("destination", currentReporterElement);
               final int currentReporterDestinationsCount = currentReporterDestinations.getLength();
               Destination currentDestination = null;
               Element currentDestinationElement = null;
               Properties currentDestinationProperties = null;
               Properties currentDestinationPeriodsAsProperties = null;
               Set<Period> currentDestinationPeriodSet = null;
               for (int j = 0; j < currentReporterDestinationsCount; j++) {
                  currentDestinationElement = (Element) currentReporterDestinations.item(j);
                  final String enabled = currentDestinationElement.getAttribute("enabled");

                  if (enabled == null || "".equals(enabled) || Boolean.parseBoolean(enabled)) { // ignore disabled destinations
                     String destClass = currentDestinationElement.getAttribute("class");
                     if (destClass.indexOf(".") < 0) {
                        destClass = DEFAULT_DESTINATION_PACKAGE + "." + destClass;
                     }
                     log.info(" '- Destination (" + destClass + ")");
                     currentDestinationProperties = getPropertiesFromSubNodes(currentDestinationElement);
                     Utils.logProperties(log, Level.DEBUG, currentDestinationProperties, "  '- ");
                     currentDestination = (Destination) ObjectFactory.summonInstance(destClass, currentDestinationProperties);
                     currentDestinationPeriodsAsProperties = getPropertiesFromSubNodes(currentDestinationElement, "period", "type", "value");
                     currentDestinationPeriodSet = new HashSet<>();
                     for (final Entry<Object, Object> entry : currentDestinationPeriodsAsProperties.entrySet()) {
                        currentDestinationPeriodSet.add(new Period(PeriodType.valueOf(((String) entry.getKey()).toUpperCase()), Long.valueOf(entry.getValue().toString())));
                     }
                     currentReporter.registerDestination(currentDestination, currentDestinationPeriodSet);
                  }
               }
               reportManager.registerReporter(currentReporter);
            }
         }
      } catch (XPathExpressionException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
         throw new PerfCakeException("Cannot parse reporting configuration: ", e);
      }

      return reportManager;
   }

   public ValidatorManager parseValidation() throws PerfCakeException {
      final ValidatorManager validatorManager = new ValidatorManager();

      log.info("--- Validation ---");
      try {

         final Element validationElement = (Element) (xPathEvaluate("validation", scenarioNode)).item(0);
         if (validationElement != null) {
            final NodeList validatorNodes = xPathEvaluate("validator", validationElement);
            Element validatorElement = null;

            String validatorId = null;
            String validatorClass = null;

            final int validatorNodesCount = validatorNodes.getLength();

            for (int i = 0; i < validatorNodesCount; i++) {
               validatorElement = (Element) validatorNodes.item(i);
               validatorId = validatorElement.getAttribute("id");
               validatorClass = validatorElement.getAttribute("class");
               if (validatorClass.indexOf(".") < 0) {
                  validatorClass = DEFAULT_VALIDATION_PACKAGE + "." + validatorClass;
               }

               final MessageValidator messageValidator = (MessageValidator) Class.forName(validatorClass, false, ScenarioExecution.class.getClassLoader()).newInstance();
               messageValidator.setAssertions(validatorNodes.item(i), "1");// add validator to validator mgr coll

               validatorManager.addValidator(validatorId, messageValidator);
               validatorManager.setEnabled(true);
            }
         }
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse validation configuration: ", e);
      }

      return validatorManager;
   }

   public Properties parseScenarioProperties() throws PerfCakeException {
      log.info("--- Scenario properties ---");
      try {
         return getPropertiesFromSubNodes(xPathEvaluate("properties", scenarioNode).item(0));
      } catch (final XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse scenario properties configuration: ", e);
      }
   }

   /**
    * @param xPathExpression
    * @param node
    * @return
    * @throws XPathExpressionException
    */
   private static NodeList xPathEvaluate(final String xPathExpression, final Node node) throws XPathExpressionException {
      return ((NodeList) xpath.evaluate(xPathExpression, node, XPathConstants.NODESET));
   }

   /**
    * @param node
    * @return
    * @throws XPathExpressionException
    */
   private static Properties getPropertiesFromSubNodes(final Node node) throws XPathExpressionException {
      return getPropertiesFromSubNodes(node, "property", "name", "value");
   }

   /**
    * @param node
    * @param propertyTagNameAttribute
    * @param propertyNameAttribute
    * @param propertyValueAttribute
    * @return
    * @throws XPathExpressionException
    */
   private static Properties getPropertiesFromSubNodes(final Node node, final String propertyTagNameAttribute, final String propertyNameAttribute, final String propertyValueAttribute) throws XPathExpressionException {
      final Properties properties = new Properties();
      final NodeList propertyNodes = xPathEvaluate(propertyTagNameAttribute, node);
      final int propertyNodesCount = propertyNodes.getLength();
      Element currentPropertyElement = null;
      for (int i = 0; i < propertyNodesCount; i++) {
         currentPropertyElement = (Element) propertyNodes.item(i);
         properties.put(currentPropertyElement.getAttribute(propertyNameAttribute), currentPropertyElement.getAttribute(propertyValueAttribute));
      }
      return properties;
   }

}
