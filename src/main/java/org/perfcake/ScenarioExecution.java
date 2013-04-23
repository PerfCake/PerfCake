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

package org.perfcake;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.message.Message;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.generator.AbstractMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.validation.MessageValidator;
import org.perfcake.validation.ValidatorManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class ScenarioExecution {

   private static final XPath xpath;

   private static final File resourcesDir = new File("src/main/resources");

   private static final String GENERATOR_PACKAGE = "org.perfcake.message.generator";

   private static final String SENDER_PACKAGE = "org.perfcake.message.sender";

   private static final String REPORTER_PACKAGE = "org.perfcake.reporting.reporters";

   private static final String DESTINATION_PACKAGE = "org.perfcake.reporting.destinations";

   private static final String VALIDATION_PACKAGE = "org.perfcake.validation";

   public static final Logger log = Logger.getLogger(ScenarioExecution.class);

   static {
      xpath = XPathFactory.newInstance().newXPath();
   }

   /**
    * It takes a string and replaces all ${&lt;property.name&gt;} placeholders
    * by respective value of the property named &lt;property.name&gt; using {@link #getProperty(String)} method.
    * 
    * @param text
    *           Original string.
    * @return Filtered string with.
    * @throws IOException
    */
   public static String filterProperties(String text) throws IOException {
      String filteredString = new String(text);
      String propertyPattern = "\\$\\{([^\\$\\{:]+)(:[^\\$\\{:]*)?}";
      Matcher matcher = Pattern.compile(propertyPattern).matcher(filteredString);

      while (matcher.find()) {
         String pValue = null;
         String pName = matcher.group(1);
         String defaultValue = null;
         if (matcher.groupCount() == 2 && matcher.group(2) != null) {
            defaultValue = (matcher.group(2)).substring(1);
         }
         pValue = getProperty(pName, defaultValue);
         if (pValue != null) {
            filteredString = filteredString.replaceAll(Pattern.quote(matcher.group()), pValue);
         }
      }
      return filteredString;
   }

   /**
    * Returns a property value. First it looks at system properties using {@link System#getProperty(String)} if the system property does not exist
    * it looks at environment variables using {@link System#getenv(String)}. If
    * the variable does not exist the method returns a <code>null</code>.
    * 
    * @param name
    *           Property name
    * @return Property value or <code>null</code>.
    */
   public static String getProperty(String name) {
      return getProperty(name, null);
   }

   /**
    * Returns a property value. First it looks at system properties using {@link System#getProperty(String)} if the system property does not exist
    * it looks at environment variables using {@link System#getenv(String)}. If
    * the variable does not exist the method returns <code>defautValue</code>.
    * 
    * @param name
    *           Property name
    * @param defaultValue
    *           Default property value
    * @return Property value or <code>defaultValue</code>.
    */
   public static String getProperty(String name, String defaultValue) {
      if (System.getProperty(name) != null) {
         return System.getProperty(name);
      } else if (System.getenv(name) != null) {
         return System.getenv(name);
      } else {
         return defaultValue;
      }
   }

   public static void main(String[] args) {

      String scenario = getProperty("scenario");
      if (scenario == null) {
         throw new PerfCakeException("scenario property is not set... please use -Dscenario=<scenario> name to specify a scenario.");
      }
      String scenarioXmlString = null;

      Scanner scanner = null;
      try {
         File scenarioFile = new File(getProperty("perfcake.scenarios.dir", resourcesDir.getCanonicalPath() + "/scenarios") + "/" + scenario + ".xml");
         if (log.isDebugEnabled()) {
            log.debug("Parsing scenario definition from file: " + scenarioFile.getCanonicalPath());
         }
         scanner = new Scanner(scenarioFile);
         scenarioXmlString = filterProperties(scanner.useDelimiter("\\Z").next());
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (scanner != null) {
            scanner.close();
         }
      }

      String serverHost = getProperty("server.host");
      String serverPort = getProperty("server.port");

      println("\n=== PerfCake scenario execution ===");
      println("Server host: " + serverHost);
      println("Server port: " + serverPort);

      try {
         Node performanceNode = parsePerformanceNode(scenarioXmlString);

         // generator
         AbstractMessageGenerator generator = parseGenerator(performanceNode);

         // sender
         MessageSenderManager messageSenderManager = parseSender(Integer.valueOf(generator.getProperty("threads")), performanceNode);

         // TODO implement message validation
         // Marek Baluch <baluchw@gmail.com>: Parse sender response assertions
         // def rv = executionCmd.sender[0].responseValidation[0]
         // def messageValidator = rv ? new
         // RulesMessageValidator(rv.text().split('\n')) : null

         // reporting
         ReportManager reportManager = parseReporting(performanceNode);

         // messages
         List<MessageToSend> messageStore = parseMessages(performanceNode);

         // TODO implement message validation
         // msm.setMessageValidator(messageValidator);

         // validation
         parseValidation(performanceNode);

         messageSenderManager.setReportManager(reportManager);
         generator.setReportManager(reportManager);
         generator.init(messageSenderManager, messageStore);

         ValidatorManager.startValidation();
         generator.generate();
         generator.close();
         ValidatorManager.setFinished(true);

      } catch (Exception e) {
         e.printStackTrace();
      }
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
   private static Node parsePerformanceNode(String scenarioXmlString) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
      Document scenarioDOM;
      scenarioDOM = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(scenarioXmlString.getBytes()));

      Node quickstartNode = xPathEvaluate("/scenario/quickstart", scenarioDOM).item(0);
      if (quickstartNode != null) {
         println("Quickstart: " + ((Element) quickstartNode).getAttribute("name"));
      }
      Node performanceNode = xPathEvaluate("/scenario/execution/performance", scenarioDOM).item(0);
      return performanceNode;
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
   private static List<MessageToSend> parseMessages(Node performanceNode) throws XPathExpressionException, IOException, FileNotFoundException {
      List<MessageToSend> messageStore = new ArrayList<MessageToSend>();
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

      println("\n--- Messages ---");
      File messagesDir = new File(getProperty("perfcake.messages.dir", resourcesDir.getCanonicalPath() + "/messages"));
      for (int i = 0; i < messageNodesCount; i++) {
         currentMessageElement = (Element) messageNodes.item(i);
         File payloadFile = new File(messagesDir, currentMessageElement.getAttribute("file"));
         Scanner scanner = null;

         if (payloadFile.exists()) {
            try {
               scanner = new Scanner(payloadFile);
               currentMessagePayload = filterProperties(scanner.useDelimiter("\\Z").next());
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

            println("'- Message (" + payloadFile.getCanonicalPath() + "), " + currentMessageMultiplicity + "x");
            println("  '- Properties:");
            printProperties(currentMessageProperties, "   '- ");
            println("  '- Headers:");
            printProperties(currentMessageHeaders, "   '- ");

            messageStore.add(currentMessageToSend);
         } else {
            throw new PerfCakeException("Message could not be created because file " + payloadFile.getCanonicalPath() + "} doesn't exist.");
         }
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
   private static ReportManager parseReporting(Node performanceNode) throws XPathExpressionException, InstantiationException, IllegalAccessException, ClassNotFoundException {
      println("\n--- Reporting ---");
      Element reportingElement = (Element) (xPathEvaluate("reporting", performanceNode).item(0));
      Properties reportingProperties = getPropertiesFromSubNodes(reportingElement);
      printProperties(reportingProperties);

      ReportManager reportManager = new ReportManager();
      setPropertiesOnObject(reportManager, reportingProperties);
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
         currentReporter = (Reporter) createInstance(REPORTER_PACKAGE + "." + currentReporterElement.getAttribute("class"), currentReporterProperties);

         println("'- Reporter (" + currentReporterElement.getAttribute("class") + ")");
         currentReporterDestinations = xPathEvaluate("destination", currentReporterElement);
         int currentReporterDestinationsCount = currentReporterDestinations.getLength();
         Destination currentDestination = null;
         Element currentDestinationElement = null;
         Properties currentDestinationProperties = null;
         for (int j = 0; j < currentReporterDestinationsCount; j++) {
            currentDestinationElement = (Element) currentReporterDestinations.item(j);
            println(" '- Destination (" + currentDestinationElement.getAttribute("class") + ")");
            currentDestinationProperties = getPropertiesFromSubNodes(currentDestinationElement);
            printProperties(currentDestinationProperties, "  '- ");
            currentDestination = (Destination) createInstance(DESTINATION_PACKAGE + "." + currentDestinationElement.getAttribute("class"), currentDestinationProperties);
            currentReporter.addDestination(currentDestination);
         }
         reportManager.addReporter(currentReporter);
      }
      reportManager.assertUntouchedProperties();
      return reportManager;
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
   private static MessageSenderManager parseSender(int senderPoolSize, Node performanceNode) throws XPathExpressionException {
      MessageSenderManager msm;
      Element senderElement = (Element) (xPathEvaluate("sender", performanceNode).item(0));
      String senderClass = senderElement.getAttribute("class");
      println("\n--- Sender (" + senderClass + ") ---");

      Properties senderProperties = getPropertiesFromSubNodes(senderElement);
      printProperties(senderProperties);

      msm = new MessageSenderManager();
      msm.setProperty("sender-class", SENDER_PACKAGE + "." + senderClass);
      msm.setProperty("sender-pool-size", senderPoolSize);
      for (Entry<Object, Object> sProperty : senderProperties.entrySet()) {
         msm.setProperty(sProperty.getKey(), sProperty.getValue());
      }
      return msm;
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
   private static AbstractMessageGenerator parseGenerator(Node performanceNode) throws XPathExpressionException, InstantiationException, IllegalAccessException, ClassNotFoundException {
      AbstractMessageGenerator generator;
      Element generatorElement = (Element) (xPathEvaluate("generator", performanceNode).item(0));
      String generatorClass = generatorElement.getAttribute("class");
      println("\n--- Generator (" + generatorClass + ") ---");

      int threads = Integer.valueOf(generatorElement.getAttribute("threads"));
      println("threads=" + threads);
      Properties generatorProperties = getPropertiesFromSubNodes(generatorElement);
      printProperties(generatorProperties);
      generator = (AbstractMessageGenerator) createInstance(GENERATOR_PACKAGE + "." + generatorClass, generatorProperties);
      generator.setProperty("threads", String.valueOf(threads));
      return generator;
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
    * @param className
    * @param properties
    * @return
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws ClassNotFoundException
    */
   private static ObjectWithProperties createInstance(String className, Properties properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
      ObjectWithProperties instance = (ObjectWithProperties) Class.forName(className, false, ScenarioExecution.class.getClassLoader()).newInstance();
      setPropertiesOnObject(instance, properties);
      return instance;
   }

   /**
    * @param object
    * @param properties
    */
   private static void setPropertiesOnObject(ObjectWithProperties object, Properties properties) {
      for (Entry<Object, Object> property : properties.entrySet()) {
         object.setProperty(property.getKey().toString(), property.getValue().toString());
      }
   }

   /**
    * @param properties
    */
   private static void printProperties(Properties properties) {
      printProperties(properties, "");
   }

   /**
    * @param properties
    * @param prefix
    */
   private static void printProperties(Properties properties, String prefix) {
      for (Entry<Object, Object> property : properties.entrySet()) {
         println(prefix + property.getKey() + "=" + property.getValue());
      }
   }

   /**
    * @param msg
    */
   private static void println(String msg) {
      System.out.println(msg);
   }

   /**
    * @param msg
    */
   private static void println(Object msg) {
      System.out.println(msg.toString());
   }

   private static void parseValidation(Node performanceNode) {
      println("\n--- Validation ---");
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

            MessageValidator messageValidator = (MessageValidator) Class.forName(VALIDATION_PACKAGE + "." + validatorClass, false, ScenarioExecution.class.getClassLoader()).newInstance();
            messageValidator.setAssertions(validatorNodes.item(i), "1");// add validator to validator mgr coll

            ValidatorManager.addValidator(validatorId, messageValidator);
         }
      } catch (InstantiationException ex) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(ex);
         }
      } catch (IllegalAccessException ex) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(ex);
         }
      } catch (ClassNotFoundException ex) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(ex);
         }
      } catch (XPathExpressionException ex) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(ex);
         }
      }
   }
}
