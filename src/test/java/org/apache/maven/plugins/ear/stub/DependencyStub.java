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
package org.apache.maven.plugins.ear.stub;

import java.util.Set;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Language;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;

/**
 * A minimal {@link Dependency} for the unit tests.
 */
public class DependencyStub implements Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String type;
    private final DependencyScope scope;

    public DependencyStub(
            String groupId, String artifactId, String version, String classifier, String type, DependencyScope scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
        this.scope = scope;
    }

    @Override
    public Type getType() {
        return new Type() {
            @Override
            public String id() {
                return type;
            }

            @Override
            public Language getLanguage() {
                return Language.JAVA_FAMILY;
            }

            @Override
            public String getExtension() {
                return "jar";
            }

            @Override
            public String getClassifier() {
                return null;
            }

            @Override
            public boolean isIncludesDependencies() {
                return false;
            }

            @Override
            public Set<PathType> getPathTypes() {
                return Set.of();
            }
        };
    }

    @Override
    public DependencyScope getScope() {
        return scope;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public DependencyCoordinates toCoordinates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public Version getVersion() {
        return new VersionStub(version);
    }

    @Override
    public Version getBaseVersion() {
        return getVersion();
    }

    @Override
    public String getClassifier() {
        return classifier == null ? "" : classifier;
    }

    @Override
    public String getExtension() {
        return "jar";
    }

    @Override
    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }

    private static final class VersionStub implements Version {
        private final String value;

        VersionStub(String value) {
            this.value = value;
        }

        @Override
        public int compareTo(Version o) {
            return value.compareTo(o.toString());
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
