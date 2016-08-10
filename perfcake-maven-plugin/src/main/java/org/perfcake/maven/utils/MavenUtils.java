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
package org.perfcake.maven.utils;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.List;

/**
 * Utility class for PerfCake Maven plugin.
 *
 * @author vjuranek
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MavenUtils {

   /**
    * Gets Maven artifact file.
    *
    * @param repoSystem
    *       Maven repository system.
    * @param remoteRepos
    *       List of Maven remote repositories to search in.
    * @param repoSession
    *       Maven repository system session.
    * @param mavenCoords
    *       Maven artifact coordinates.
    * @param log
    *       Maven logging system.
    * @return The file with the jar file location representing the entered artifact coordinates.
    * @throws ArtifactResolutionException
    *       When it was not possible to resolve the jar file.
    */
   public static File getArtifactJarFile(final RepositorySystem repoSystem, final List<RemoteRepository> remoteRepos, final RepositorySystemSession repoSession, final String mavenCoords, final Log log) throws ArtifactResolutionException {
      Artifact artifact = new DefaultArtifact(mavenCoords);
      ArtifactRequest request = new ArtifactRequest();
      request.setArtifact(artifact);
      request.setRepositories(remoteRepos);
      log.debug("Resolving artifact " + artifact + " from " + remoteRepos);
      ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
      log.debug("Resolved artifact " + artifact + " to " + result.getArtifact().getFile() + " from " + result.getRepository());
      return result.getArtifact().getFile();
   }

}
