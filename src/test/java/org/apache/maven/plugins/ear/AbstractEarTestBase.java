/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.ear;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.plugins.ear.stub.DependencyStub;
import org.apache.maven.plugins.ear.util.ResolvedArtifact;

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractEarTestBase {

    protected static final String DEFAULT_GROUPID = "eartest";

    private static final String DEFAULT_TYPE = "jar";

    protected void setUri(EarModule module, String uri) {
        ((AbstractEarModule) module).setUri(uri);
    }

    protected Set<ResolvedArtifact> createArtifacts(String[] artifactIds) {
        return createArtifacts(artifactIds, null);
    }

    protected Set<ResolvedArtifact> createArtifacts(String[] artifactIds, String[] classifiers) {
        Set<ResolvedArtifact> result = new TreeSet<>();
        for (int i = 0; i < artifactIds.length; i++) {
            String artifactId = artifactIds[i];
            String classifier = classifiers == null ? null : classifiers[i];
            result.add(new ResolvedArtifact(
                    new DependencyStub(
                            DEFAULT_GROUPID, artifactId, "1.0", classifier, DEFAULT_TYPE, DependencyScope.COMPILE),
                    Path.of(artifactId + "-1.0.jar")));
        }
        return result;
    }

    protected ResolvedArtifact createArtifact(String artifactId, String type) {
        return new ResolvedArtifact(
                new DependencyStub(
                        DEFAULT_GROUPID,
                        artifactId,
                        "1.0",
                        null,
                        Objects.toString(type, DEFAULT_TYPE),
                        DependencyScope.COMPILE),
                Path.of(artifactId + "-1.0.jar"));
    }
}
