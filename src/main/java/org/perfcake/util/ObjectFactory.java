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

package org.perfcake.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtils;
import org.perfcake.ObjectWithProperties;
import org.perfcake.ScenarioExecution;

/**
 * 
 * TODO this will use real POJOs and reflection to assign properties!!!
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ObjectFactory {

   /**
    * @param className
    * @param properties
    * @return
    * @throws InstantiationException
    * @throws IllegalAccessException
    * @throws ClassNotFoundException
    */
   public static ObjectWithProperties createInstance(String className, Properties properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
      ObjectWithProperties instance = (ObjectWithProperties) Class.forName(className, false, ScenarioExecution.class.getClassLoader()).newInstance();
      setPropertiesOnObject(instance, properties);
      return instance;
   }

   /**
    * @param object
    * @param properties
    */
   public static void setPropertiesOnObject(ObjectWithProperties object, Properties properties) {
      for (Entry<Object, Object> property : properties.entrySet()) {
         object.setProperty(property.getKey().toString(), property.getValue().toString());
      }
   }

   public static Object summonInstance(String className, Properties properties) throws Throwable {
      Object object = Class.forName(className, false, ObjectFactory.class.getClassLoader()).newInstance();

      for (String key : properties.stringPropertyNames()) {
         BeanUtils.setProperty(object, key, properties.getProperty(key));
      }

      return object;
   }
}
