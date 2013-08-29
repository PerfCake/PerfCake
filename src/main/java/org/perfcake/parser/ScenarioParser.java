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
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.Scenario;
import org.perfcake.util.Utils;
import org.xml.sax.SAXException;

/**
 * ScenarioParser is a tool for loading XML document with scenario description from URL and producing JAXB model representation
 * TODO review logging 
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Jiří Sedláček <jiri@sedlackovi.cz>
 */
public class ScenarioParser {

   public static final Logger log = Logger.getLogger(ScenarioParser.class);

   private String scenarioConfig;   

   /**
    * 
    * @param scenario
    * @throws PerfCakeException if URL cannot be read
    */
   public ScenarioParser(final URL scenario) throws PerfCakeException {
      try {
         this.scenarioConfig = Utils.readFilteredContent(scenario);

      } catch (IOException e) {
         throw new PerfCakeException("Cannot read scenario configuration: ", e);
      }
   }

   /**
    * Do the parsing itself by using JAXB
    * @return
    * @throws PerfCakeException if XML is not valid or cannot be successfully parsed
    */
   public org.perfcake.model.Scenario parse() throws PerfCakeException {
      try {
         Source schemaFile = new StreamSource(getClass().getResourceAsStream("/schemas/perfcake-scenario-" + Scenario.VERSION + ".xsd"));
         Source scenarioXML = new StreamSource(new ByteArrayInputStream(scenarioConfig.getBytes()));
         SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         Schema schema = schemaFactory.newSchema(schemaFile);

         JAXBContext context = JAXBContext.newInstance(org.perfcake.model.Scenario.class);
         Unmarshaller unmarshaller = context.createUnmarshaller();
         unmarshaller.setSchema(schema);
         return (org.perfcake.model.Scenario) unmarshaller.unmarshal(scenarioXML);
      } catch (SAXException e) {
         throw new PerfCakeException("Cannot validate scenario configuration: ", e);
      } catch (JAXBException e) {
         throw new PerfCakeException("Cannot parse scenario configuration: ", e);
      }
   }
}
