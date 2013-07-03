/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.parser;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.ScenarioExecution;
import org.perfcake.message.Message;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.ObjectFactory;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidatorManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * TODO review logging and refactor parsing, remove duplicated code
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ScenarioParser implements Closeable {

   private static final String GENERATOR_PACKAGE = "org.perfcake.message.generator";
   private static final String SENDER_PACKAGE = "org.perfcake.message.sender";
   private static final String REPORTER_PACKAGE = "org.perfcake.reporting.reporters";
   private static final String DESTINATION_PACKAGE = "org.perfcake.reporting.destinations";
   private static final String VALIDATION_PACKAGE = "org.perfcake.validation";

   public static final Logger log = Logger.getLogger(ScenarioExecution.class);

   private static final XPath xpath = XPathFactory.newInstance().newXPath();

   private InputStream scenarioStream;
   private Node performanceNode;

   public ScenarioParser(URL scenario) throws PerfCakeException {
      try {
         this.scenarioStream = scenario.openStream();
      } catch (IOException e) {
         throw new PerfCakeException("Cannot read scenario configuration: ", e);
      }

      this.performanceNode = parsePerformanceNode(scenario);
   }

   public void close() throws IOException {
      scenarioStream.close();
   }

   /**
    * Parse the DOM node representation of the <code>performance</code> element
    * of the scenario's XML definition.
    * 
    * @param scenarioXmlString
    *           String containing the scenario's XML definition.
    * @return DOM representation of the <code>performance</code> element of the
    *         scenario's definition.
    * @throws SAXException
    * @throws IOException
    * @throws ParserConfigurationException
    * @throws XPathExpressionException
    */
   private Node parsePerformanceNode(URL scenario) throws PerfCakeException {
      Document scenarioDOM;
      Node node = null;

      try {
         scenarioDOM = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(scenarioStream);
         node = xPathEvaluate("/scenario/execution/performance", scenarioDOM).item(0);
      } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse scenario configuration: ", e);
      }

      return node;
   }

   /**
    * Parse the <code>sender</code> element into a {@link AbstractMessageGenerator} instance.
    * 
    * @param performanceNode
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
         Element generatorElement = (Element) (xPathEvaluate("generator", performanceNode).item(0));
         String generatorClass = generatorElement.getAttribute("class");
         if (generatorClass.indexOf(".") < 0) {
            generatorClass = GENERATOR_PACKAGE + "." + generatorClass;
         }
         log.info("--- Generator (" + generatorClass + ") ---");

         int threads = Integer.valueOf(generatorElement.getAttribute("threads"));
         log.info("  threads=" + threads);

         Properties generatorProperties = getPropertiesFromSubNodes(generatorElement);
         Utils.logProperties(log, Level.DEBUG, generatorProperties, "   ");

         generator = (AbstractMessageGenerator) ObjectFactory.createInstance(generatorClass, generatorProperties);
         generator.setProperty("threads", String.valueOf(threads));
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse message generator configuration: ", e);
      }

      return generator;
   }

   /**
    * Parse the <code>sender</code> element into a {@link MessageSenderManager} instance.
    * 
    * @param senderPoolSize
    *           Size of the message sender pool.
    * @param performanceNode
    *           DOM representation of the <code>performance</code> element of
    *           the scenario's definition.
    * @return A message sender manager.
    * @throws XPathExpressionException
    */
   public MessageSenderManager parseSender(int senderPoolSize) throws PerfCakeException {
      MessageSenderManager msm;

      try {
         Element senderElement = (Element) (xPathEvaluate("sender", performanceNode).item(0));
         String senderClass = senderElement.getAttribute("class");
         if (senderClass.indexOf(".") < 0) {
            senderClass = SENDER_PACKAGE + "." + senderClass;
         }
         log.info("--- Sender (" + senderClass + ") ---");

         Properties senderProperties = getPropertiesFromSubNodes(senderElement);
         Utils.logProperties(log, Level.DEBUG, senderProperties, "   ");

         msm = new MessageSenderManager();
         msm.setProperty("sender-class", senderClass);
         msm.setProperty("sender-pool-size", senderPoolSize);
         for (Entry<Object, Object> sProperty : senderProperties.entrySet()) {
            msm.setProperty(sProperty.getKey(), sProperty.getValue());
         }
      } catch (XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse message sender manager configuration: ", e);
      }

      return msm;
   }

   /**
    * Parse the <code>messages</code> element into a message store.
    * 
    * @param performanceNode
    *           DOM representation of the <code>performance</code> element of
    *           the scenario's definition.
    * @return Message store in a form of {@link Map}&lt;{@link Message}, {@link Long}&gt; where the keys are stored messages and the values
    *         are multiplicity of how many times the message is sent in a single
    *         iteration.
    * @throws XPathExpressionException
    * @throws IOException
    * @throws FileNotFoundException
    */
   public List<MessageToSend> parseMessages() throws PerfCakeException {
      List<MessageToSend> messageStore = new ArrayList<>();

      try {
         Element messagesElement = (Element) (xPathEvaluate("messages", performanceNode)).item(0);

         NodeList messageNodes = xPathEvaluate("message", messagesElement);
         int messageNodesCount = messageNodes.getLength();
         Message currentMessage = null;
         MessageToSend currentMessageToSend = null;
         Element currentMessageElement = null;
         String currentMessagePayload = null;
         String currentMessageMultiplicityAttributeValue = null;
         String currentMessageValidatorIdAttributeValue = null;
         String currentMessageValidatorId = null;
         Properties currentMessageProperties = null;
         Properties currentMessageHeaders = null;
         long currentMessageMultiplicity = -1;

         log.info("--- Messages ---");
         File messagesDir = new File(Utils.getProperty("perfcake.messages.dir", Utils.resourcesDir.getAbsolutePath() + "/messages"));
         for (int i = 0; i < messageNodesCount; i++) {
            currentMessageElement = (Element) messageNodes.item(i);
            File payloadFile = new File(messagesDir, currentMessageElement.getAttribute("file"));
            Scanner scanner = null;

            /// TODO support for URL based message locations
            if (payloadFile.exists()) {
               try {
                  scanner = new Scanner(payloadFile);
                  currentMessagePayload = Utils.filterProperties(scanner.useDelimiter("\\Z").next());
               } finally {
                  if (scanner != null) {
                     scanner.close();
                  }
               }
               currentMessageProperties = getPropertiesFromSubNodes(currentMessageElement);
               currentMessageHeaders = getPropertiesFromSubNodes(currentMessageElement, "header", "name", "value");

               currentMessage = new Message(currentMessagePayload);
               currentMessage.setProperties(currentMessageProperties);
               currentMessage.setHeaders(currentMessageHeaders);

               currentMessageMultiplicityAttributeValue = currentMessageElement.getAttribute("multiplicity");
               if (currentMessageMultiplicityAttributeValue.equals("")) {
                  currentMessageMultiplicity = 1l;
               } else {
                  currentMessageMultiplicity = Long.valueOf(currentMessageMultiplicityAttributeValue);
               }

               currentMessageValidatorIdAttributeValue = currentMessageElement.getAttribute("validatorId");
               if (currentMessageValidatorIdAttributeValue.equals("")) {
                  currentMessageValidatorId = null;
               } else {
                  currentMessageValidatorId = currentMessageValidatorIdAttributeValue;
               }

               // create message to be send
               currentMessageToSend = new MessageToSend(currentMessage, currentMessageMultiplicity, currentMessageValidatorId);

               log.info("'- Message (" + payloadFile.getCanonicalPath() + "), " + currentMessageMultiplicity + "x");
               if (log.isDebugEnabled()) {
                  log.debug("  '- Properties:");
                  Utils.logProperties(log, Level.DEBUG, currentMessageProperties, "   '- ");
                  log.debug("  '- Headers:");
                  Utils.logProperties(log, Level.DEBUG, currentMessageHeaders, "   '- ");
               }

               messageStore.add(currentMessageToSend);
            } else {
               throw new IOException("Message could not be created because file " + payloadFile.getCanonicalPath() + " doesn't exist.");
            }
         }
      } catch (XPathExpressionException | IOException e) {
         throw new PerfCakeException("Cannot parse messages configuration: ", e);
      }
      return messageStore;
   }

   /**
    * Parse the <code>reporting</code> element into a {@link ReportManager} instance.
    * 
    * @param performanceNode
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
         Element reportingElement = (Element) (xPathEvaluate("reporting", performanceNode).item(0));
         Properties reportingProperties = getPropertiesFromSubNodes(reportingElement);
         Utils.logProperties(log, Level.DEBUG, reportingProperties, "   ");

         ObjectFactory.setPropertiesOnObject(reportManager, reportingProperties);
         reportManager.loadConfigValues();

         NodeList reporterNodes = xPathEvaluate("reporter", reportingElement);
         int reporterNodesCount = reporterNodes.getLength();
         Reporter currentReporter = null;
         Element currentReporterElement = null;
         Properties currentReporterProperties = null;
         NodeList currentReporterDestinations = null;

         for (int i = 0; i < reporterNodesCount; i++) {
            currentReporterElement = (Element) reporterNodes.item(i);
            currentReporterProperties = getPropertiesFromSubNodes(currentReporterElement);
            String reportClass = currentReporterElement.getAttribute("class");
            if (reportClass.indexOf(".") < 0) {
               reportClass = REPORTER_PACKAGE + "." + reportClass;
            }
            currentReporter = (Reporter) ObjectFactory.createInstance(reportClass, currentReporterProperties);

            log.info("'- Reporter (" + reportClass + ")");
            currentReporterDestinations = xPathEvaluate("destination", currentReporterElement);
            int currentReporterDestinationsCount = currentReporterDestinations.getLength();
            Destination currentDestination = null;
            Element currentDestinationElement = null;
            Properties currentDestinationProperties = null;
            for (int j = 0; j < currentReporterDestinationsCount; j++) {
               currentDestinationElement = (Element) currentReporterDestinations.item(j);
               String destClass = currentDestinationElement.getAttribute("class");
               if (destClass.indexOf(".") < 0) {
                  destClass = DESTINATION_PACKAGE + "." + destClass;
               }
               log.info(" '- Destination (" + destClass + ")");
               currentDestinationProperties = getPropertiesFromSubNodes(currentDestinationElement);
               Utils.logProperties(log, Level.DEBUG, currentDestinationProperties, "  '- ");
               currentDestination = (Destination) ObjectFactory.createInstance(destClass, currentDestinationProperties);
               currentReporter.addDestination(currentDestination);
            }
            reportManager.addReporter(currentReporter);
         }
         reportManager.assertUntouchedProperties();
      } catch (XPathExpressionException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
         throw new PerfCakeException("Cannot parse reporting configuration: ", e);
      }

      return reportManager;
   }

   public void parseValidation() throws PerfCakeException {
      log.info("\n--- Validation ---");
      try {

         Element validationElement = (Element) (xPathEvaluate("validation", performanceNode)).item(0);
         NodeList validatorNodes = xPathEvaluate("validator", validationElement);
         Element validatorElement = null;

         String validatorId = null;
         String validatorClass = null;

         int validatorNodesCount = validatorNodes.getLength();

         for (int i = 0; i < validatorNodesCount; i++) {
            validatorElement = (Element) validatorNodes.item(i);
            validatorId = validatorElement.getAttribute("id");
            validatorClass = validatorElement.getAttribute("class");
            if (validatorClass.indexOf(".") < 0) {
               validatorClass = VALIDATION_PACKAGE + "." + validatorClass;
            }

            MessageValidator messageValidator = (MessageValidator) Class.forName(validatorClass, false, ScenarioExecution.class.getClassLoader()).newInstance();
            messageValidator.setAssertions(validatorNodes.item(i), "1");// add validator to validator mgr coll

            ValidatorManager.addValidator(validatorId, messageValidator);
         }
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | XPathExpressionException e) {
         throw new PerfCakeException("Cannot parse validation configuration: ", e);
      }
   }
   
   
   /**
    * @param xPathExpression
    * @param node
    * @return
    * @throws XPathExpressionException
    */
   private static NodeList xPathEvaluate(String xPathExpression, Node node) throws XPathExpressionException {
      return ((NodeList) xpath.evaluate(xPathExpression, node, XPathConstants.NODESET));
   }

   /**
    * @param node
    * @return
    * @throws XPathExpressionException
    */
   private static Properties getPropertiesFromSubNodes(Node node) throws XPathExpressionException {
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
   private static Properties getPropertiesFromSubNodes(Node node, String propertyTagNameAttribute, String propertyNameAttribute, String propertyValueAttribute) throws XPathExpressionException {
      Properties properties = new Properties();
      NodeList propertyNodes = xPathEvaluate(propertyTagNameAttribute, node);
      int propertyNodesCount = propertyNodes.getLength();
      Element currentPropertyElement = null;
      for (int i = 0; i < propertyNodesCount; i++) {
         currentPropertyElement = (Element) propertyNodes.item(i);
         properties.put(currentPropertyElement.getAttribute(propertyNameAttribute), currentPropertyElement.getAttribute(propertyValueAttribute));
      }
      return properties;
   }

}
