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
package org.perfcake;

import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Does the necessary configurations for the tests to work.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TestSetup {

   private static final Logger log = LogManager.getLogger(TestSetup.class);

   /**
    * Determines whether the system running this application is POSIX compliant.
    */
   private static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

   @BeforeClass(alwaysRun = true)
   public void configureLocations() throws Exception {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, Utils.getResource("/scenarios"));
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.getResource("/messages"));
   }

   @AfterClass(alwaysRun = true)
   public void unsetLocations() {
      System.clearProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY);
      System.clearProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY);
   }

   /**
    * Creates a temporary directory based on the given name. It also sets correct rights on POSIX compliant systems. The directory gets deleted at the end of this application process.
    *
    * @param name
    *       Name of the temporary directory.
    * @return The full path of the created directory.
    */
   public static String createTempDir(final String name) {
      try {
         if (isPosix) {
            final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr--");
            final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
            return Files.createTempDirectory(name, attr).toString();
         } else {
            return Files.createTempDirectory(name).toString();
         }
      } catch (final IOException e) {
         log.error(String.format("Cannot create temporary directory %s: ", name), e);
         return null;
      }
   }

}
