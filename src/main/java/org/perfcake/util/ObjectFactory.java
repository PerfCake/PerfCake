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

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.log4j.Logger;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ObjectFactory {

   private static final Logger log = Logger.getLogger(ObjectFactory.class);

   private static class EnumConvertUtilsBean extends ConvertUtilsBean {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      @Override
      public Object convert(String value, Class clazz) {
         if (clazz.isEnum()) {
            return Enum.valueOf(clazz, Utils.camelCaseToEnum(value));
         } else {
            return super.convert(value, clazz);
         }
      }

   }

   /**
    * @param object
    * @param properties
    * @throws InvocationTargetException
    * @throws IllegalAccessException
    */
   public static void setPropertiesOnObject(Object object, Properties properties) throws IllegalAccessException, InvocationTargetException {
      BeanUtilsBean beanUtilsBean = new BeanUtilsBean(new EnumConvertUtilsBean());
      for (String key : properties.stringPropertyNames()) {
         if (log.isTraceEnabled()) {
            log.trace("Setting property: '" + key + "'='" + properties.getProperty(key) + "'");
         }
         beanUtilsBean.setProperty(object, key, properties.getProperty(key));
         // BeanUtils.setProperty(object, key, properties.getProperty(key));
      }
   }

   public static Object summonInstance(String className, Properties properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
      Object object = Class.forName(className, false, ObjectFactory.class.getClassLoader()).newInstance();
      setPropertiesOnObject(object, properties);

      return object;
   }
}
