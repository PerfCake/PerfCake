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
package org.perfcake.util;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.log4j.Logger;
import org.perfcake.PerfCakeConst;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ObjectFactory {

   private static final Logger log = Logger.getLogger(ObjectFactory.class);
   private static ClassLoader pluginClassLoader = null;

   private static class EnumConvertUtilsBean extends ConvertUtilsBean {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      @Override
      public Object convert(final String value, final Class clazz) {
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
   public static void setPropertiesOnObject(final Object object, final Properties properties) throws IllegalAccessException, InvocationTargetException {
      BeanUtilsBean beanUtilsBean = new BeanUtilsBean(new EnumConvertUtilsBean());
      for (String key : properties.stringPropertyNames()) {
         if (log.isTraceEnabled()) {
            log.trace("Setting property: '" + key + "'='" + properties.getProperty(key) + "'");
         }
         beanUtilsBean.setProperty(object, key, properties.getProperty(key));
         // BeanUtils.setProperty(object, key, properties.getProperty(key));
      }
   }

   public static Object summonInstance(final String className, final Properties properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
      Object object = Class.forName(className, false, getPluginClassLoader()).newInstance();
      setPropertiesOnObject(object, properties);

      return object;
   }

   protected static ClassLoader getPluginClassLoader() {
      if (pluginClassLoader == null) {
         final ClassLoader currentClassLoader = ObjectFactory.class.getClassLoader();
         final String pluginsDirProp = Utils.getProperty(PerfCakeConst.PLUGINS_DIR_PROPERTY);
         if (pluginsDirProp == null) {
            return currentClassLoader;
         }

         final File pluginsDir = new File(pluginsDirProp);
         final File[] plugins = pluginsDir.listFiles(new FileExtensionFilter(".jar"));

         if ((plugins == null) || (plugins.length == 0)) {
            return currentClassLoader;
         }

         final URL[] pluginURLs = new URL[plugins.length];
         for (int i = 0; i < plugins.length; i++) {
            try {
               pluginURLs[i] = plugins[i].toURI().toURL();
            } catch (MalformedURLException e) {
               log.warn(String.format("Cannot resolve path to plugin '%s', skipping this file", plugins[i]));
            }
         }

         AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
               pluginClassLoader = new URLClassLoader(pluginURLs, currentClassLoader);
               return null;
            }
         });
      }

      return pluginClassLoader;
   }

   private static class FileExtensionFilter implements FilenameFilter {
      private final String extension;

      public FileExtensionFilter(String extension) {
         this.extension = extension;
      }

      public boolean accept(File dir, String name) {
         return name.endsWith(extension) ? true : false;
      }
   }

}
