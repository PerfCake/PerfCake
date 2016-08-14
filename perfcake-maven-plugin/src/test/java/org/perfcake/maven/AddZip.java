/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class AddZip {
   private List<String> fileList = new ArrayList<>();
   private final String srcFolder;
   private final String outputFile;

   public AddZip(final String srcFolder, final String outputFile) {
      this.srcFolder = srcFolder;
      this.outputFile = outputFile;
   }

   public void compress() throws IOException {
      generateFileList(new File(srcFolder));
      zipIt(outputFile);
   }

   private void zipIt(String zipFile) throws IOException {
      byte[] buffer = new byte[1024];
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);
      int prefix = new File(srcFolder).getAbsolutePath().length() + 1;

      for (String file : this.fileList) {
         ZipEntry ze = new ZipEntry(file.substring(prefix));
         zos.putNextEntry(ze);

         FileInputStream in = new FileInputStream(file);

         int len;
         while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
         }

         in.close();
      }

      zos.closeEntry();
      zos.close();
   }

   private void generateFileList(File node) {
      if (node.isFile()) {
         fileList.add(node.getAbsolutePath());
      }

      if (node.isDirectory()) {
         String[] subNode = node.list();
         for (String filename : subNode) {
            generateFileList(new File(node, filename));
         }
      }
   }
}
