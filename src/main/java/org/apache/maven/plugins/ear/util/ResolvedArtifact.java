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
package org.apache.maven.plugins.ear.util;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

import org.apache.maven.api.Dependency;

/**
 * A dependency of the project together with the file it was resolved to. The Maven 4 API keeps those apart (a
 * {@link Dependency} carries no file), whereas the Maven 3 {@code Artifact} carried both.
 */
public final class ResolvedArtifact implements Comparable<ResolvedArtifact> {

    private static final Comparator<ResolvedArtifact> ORDER = Comparator.comparing(ResolvedArtifact::getGroupId)
            .thenComparing(ResolvedArtifact::getArtifactId)
            .thenComparing(ResolvedArtifact::getType)
            .thenComparing(a -> String.valueOf(a.getClassifier()))
            .thenComparing(a -> a.getVersion());

    private final Dependency dependency;

    private final Path path;

    /**
     * @param dependency the dependency
     * @param path the file the dependency was resolved to
     */
    public ResolvedArtifact(Dependency dependency, Path path) {
        this.dependency = Objects.requireNonNull(dependency, "dependency");
        this.path = path;
    }

    /**
     * @return the dependency; it can be used wherever an {@link org.apache.maven.api.Artifact} is expected
     */
    public Dependency getDependency() {
        return dependency;
    }

    /**
     * @return the resolved file
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return the group id
     */
    public String getGroupId() {
        return dependency.getGroupId();
    }

    /**
     * @return the artifact id
     */
    public String getArtifactId() {
        return dependency.getArtifactId();
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return dependency.getVersion().toString();
    }

    /**
     * @return the classifier, or {@code null} if there is none (the Maven 4 API returns an empty string)
     */
    public String getClassifier() {
        String classifier = dependency.getClassifier();
        return classifier == null || classifier.isEmpty() ? null : classifier;
    }

    /**
     * @return the dependency type as declared, for example {@code ejb}, {@code war} or {@code sar}
     */
    public String getType() {
        return dependency.getType().id();
    }

    /**
     * @return the dependency scope, for example {@code compile}
     */
    public String getScope() {
        return dependency.getScope().id();
    }

    /**
     * @return {@code true} if the dependency is optional
     */
    public boolean isOptional() {
        return dependency.isOptional();
    }

    /**
     * @return {@code true} if the dependency is a snapshot
     */
    public boolean isSnapshot() {
        return dependency.isSnapshot();
    }

    @Override
    public int compareTo(ResolvedArtifact other) {
        return ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResolvedArtifact && compareTo((ResolvedArtifact) o) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroupId(), getArtifactId(), getType(), getClassifier(), getVersion());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getGroupId()).append(':').append(getArtifactId());
        sb.append(':').append(getType());
        if (getClassifier() != null) {
            sb.append(':').append(getClassifier());
        }
        return sb.append(':').append(getVersion()).toString();
    }
}
