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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.plugins.ear.util.ArtifactTypeMappingService;
import org.apache.maven.plugins.ear.util.JavaEEVersion;
import org.apache.maven.plugins.ear.util.ResolvedArtifact;
import org.apache.maven.plugins.ear.util.XmlConfig;

/**
 * A base class for EAR-processing related tasks.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractEarMojo implements Mojo {
    /**
     * The application XML URI {@code META-INF/application.xml}
     */
    public static final String APPLICATION_XML_URI = "META-INF/application.xml";

    /**
     * The {@code META-INF} folder.
     */
    public static final String META_INF = "META-INF";

    /**
     * UTF-8 encoding constant.
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * The schema version of generated <code>application.xml</code> descriptor.
     * <p>
     * Valid values are 1.3, 1.4, 5, 6, 7, 8, 9, 10 and 11.
     * <p>
     * See: <a href="https://jakarta.ee/xml/ns/jakartaee/">Jakarta EE XML Schemas</a>
     */
    @Parameter(defaultValue = "7")
    protected String version;

    /**
     * Character encoding for the auto-generated deployment file(s).
     */
    @Parameter(defaultValue = "UTF-8")
    protected String encoding;

    /**
     * Directory where the deployment descriptor file(s) will be auto-generated.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected String generatedDescriptorLocation;

    /**
     * The maven project.
     */
    @Inject
    protected Project project;

    /**
     * The Maven session.
     */
    @Inject
    protected Session session;

    @Inject
    private Log log;

    @Inject
    private MojoExecution mojoExecution;

    /**
     * The ear modules configuration.
     */
    @Parameter
    private EarModule[] modules;

    /**
     * The artifact type mappings.
     */
    @Parameter
    protected XmlNode artifactTypeMappings;

    /**
     * The default bundle dir for libraries.
     */
    @Parameter
    protected String defaultLibBundleDir;

    /**
     * Should libraries be added in application.xml.
     */
    @Parameter(defaultValue = "false")
    private Boolean includeLibInApplicationXml = Boolean.FALSE;

    /**
     * Only here to identify migration issues. The usage of this parameter will fail the build.
     *
     * @deprecated use {@link #outputFileNameMapping}
     */
    @Deprecated
    @Parameter
    private String fileNameMapping;

    /**
     * The file name mapping to use for all dependencies included in the EAR file. The mapping between artifacts and the
     * file names which is used within the EAR file.
     * See
     * <a href="./examples/customize-file-name-mapping.html">Customizing The File Name Mapping</a>.
     *
     * @since 3.0.0
     */
    @Parameter(
            defaultValue = "@{groupId}@-@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@",
            required = true)
    private String outputFileNameMapping;

    /**
     * Directory that resources are copied to during the build.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
    private File workDirectory;

    /**
     * The JBoss specific configuration.
     */
    @Parameter
    private XmlNode jboss;

    /**
     * The id to use to define the main artifact (e.g. the artifact without a classifier) when there are multiple
     * candidates.
     */
    @Parameter
    private String mainArtifactId = "none";

    /**
     * The dependency scopes considered for the EAR, the Maven 3 {@code compile+runtime} artifact filter.
     */
    private static final List<String> PACKAGED_SCOPES = Arrays.asList("compile", "runtime", "provided", "system");

    /**
     * The dependency scopes that make a module part of the runtime class path, the Maven 3 {@code runtime} artifact
     * filter.
     */
    private static final List<String> RUNTIME_SCOPES = Arrays.asList("compile", "runtime");

    private List<EarModule> earModules;

    private List<EarModule> allEarModules;

    private List<EarModule> providedEarModules;

    private JbossConfiguration jbossConfiguration;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoException {
        if (fileNameMapping != null) {
            log.error("fileNameMapping has been removed with version 3.0.0. You are still using it.");
            log.error("Use outputFileNameMapping instead.");
            throw new MojoException(
                    "fileNameMapping has been removed with version 3.0.0 " + "but you are still using it.");
        }

        final JavaEEVersion javaEEVersion = JavaEEVersion.getJavaEEVersion(version);
        log.debug("Resolving artifact type mappings ...");
        ArtifactTypeMappingService typeMappingService;
        try {
            typeMappingService = new ArtifactTypeMappingService();
            typeMappingService.configure(getConfiguration("artifactTypeMappings", artifactTypeMappings));
        } catch (EarPluginException e) {
            throw new MojoException("Failed to initialize artifact type mappings", e);
        }

        log.debug("Initializing JBoss configuration if necessary ...");
        try {
            initializeJbossConfiguration();
        } catch (EarPluginException e) {
            throw new MojoException("Failed to initialize JBoss configuration", e);
        }

        final Set<ResolvedArtifact> projectArtifacts = resolveProjectArtifacts();

        log.debug("Initializing ear execution context");
        EarExecutionContext earExecutionContext = new EarExecutionContext(
                projectArtifacts,
                mainArtifactId,
                defaultLibBundleDir,
                jbossConfiguration,
                outputFileNameMapping,
                typeMappingService);

        log.debug("Resolving ear modules ...");
        List<EarModule> allModules = new ArrayList<>();
        try {
            if (modules != null) {
                // Let's validate user-defined modules
                for (EarModule module : modules) {
                    log.debug("Resolving ear module[" + module + "]");
                    module.setEarExecutionContext(earExecutionContext);
                    module.resolveArtifact(projectArtifacts);
                    allModules.add(module);
                }
            }

            // Let's add other modules
            for (ResolvedArtifact artifact : projectArtifacts) {
                // If the artifact's type is POM, ignore and continue
                // since it's used for transitive deps only.
                if ("pom".equals(artifact.getType())) {
                    continue;
                }

                // Artifact is not yet registered and it has not test scope, nor is it optional
                if (!isArtifactRegistered(artifact, allModules) && !artifact.isOptional()) {
                    EarModule module = EarModuleFactory.newEarModule(
                            artifact,
                            javaEEVersion,
                            defaultLibBundleDir,
                            includeLibInApplicationXml,
                            typeMappingService);
                    module.setEarExecutionContext(earExecutionContext);
                    allModules.add(module);
                }
            }
        } catch (EarPluginException e) {
            throw new MojoException("Failed to initialize ear modules", e);
        }

        // Now we have everything. Let's build modules which have not been excluded
        allEarModules = new ArrayList<>();
        providedEarModules = new ArrayList<>();
        earModules = new ArrayList<>();
        for (EarModule earModule : allModules) {
            if (earModule.isExcluded()) {
                log.debug("Skipping ear module[" + earModule + "]");
            } else {
                allEarModules.add(earModule);
                if (RUNTIME_SCOPES.contains(earModule.getArtifact().getScope())) {
                    earModules.add(earModule);
                } else {
                    providedEarModules.add(earModule);
                }
            }
        }
    }

    /**
     * Resolves the dependencies of the project and keeps those with a scope that ends up in an EAR: compile, runtime
     * and provided (the latter are not packaged, but they take part in the class path handling of skinny modules).
     * The resolution is done for the {@code test} path scope, like the Maven 3 based plugin did with
     * {@code requiresDependencyResolution = test}, which keeps the order of the dependencies and the mediation result.
     * <p>
     * The dependencies of a dependency whose type includes them (such as a {@code war}) are left out, as the Maven 3
     * resolution did, unless they can be reached some other way. The Maven 4 API resolution returns them, and lists
     * an artifact that can be reached by several paths only once, below the nearest, which can be the {@code war}.
     * A second, verbose, collection tells which artifacts can be reached without passing through such a dependency.
     *
     * @return the resolved artifacts in dependency order
     */
    private Set<ResolvedArtifact> resolveProjectArtifacts() {
        final DependencyResolver resolver = session.getService(DependencyResolver.class);
        final DependencyResolverResult resolved = resolver.resolve(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.RESOLVE, project, PathScope.TEST_RUNTIME));
        final Map<String, Path> paths = new HashMap<>();
        resolved.getDependencies().forEach((dependency, path) -> paths.put(dependency.key(), path));

        final Set<String> reachable = new HashSet<>();
        collectReachable(
                resolver.collect(DependencyResolverRequest.builder()
                                .session(session)
                                .requestType(DependencyResolverRequest.RequestType.COLLECT)
                                .project(project)
                                .pathScope(PathScope.TEST_RUNTIME)
                                .verbose(true)
                                .build())
                        .getRoot(),
                reachable);

        final Set<ResolvedArtifact> result = new LinkedHashSet<>();
        collectArtifacts(resolved.getRoot(), false, paths, reachable, result);
        return result;
    }

    private static void collectArtifacts(
            Node node,
            boolean nodeContributes,
            Map<String, Path> paths,
            Set<String> reachable,
            Set<ResolvedArtifact> artifacts) {
        for (Node child : node.getChildren()) {
            final Dependency dependency = child.getDependency();
            // the artifact is listed below its nearest occurrence only: whatever is below an artifact that can be
            // reached is reachable too, unless that artifact includes its dependencies
            final boolean included =
                    dependency != null && (nodeContributes || reachable.contains(coordinates(dependency)));
            final Path path = included ? paths.get(dependency.key()) : null;
            if (path != null) {
                final ResolvedArtifact artifact = new ResolvedArtifact(dependency, path);
                if (PACKAGED_SCOPES.contains(artifact.getScope())) {
                    artifacts.add(artifact);
                }
            }
            collectArtifacts(
                    child, included && !dependency.getType().isIncludesDependencies(), paths, reachable, artifacts);
        }
    }

    private static void collectReachable(Node node, Set<String> reachable) {
        for (Node child : node.getChildren()) {
            final Dependency dependency = child.getDependency();
            if (dependency != null) {
                reachable.add(coordinates(dependency));
            }
            if (dependency == null || !dependency.getType().isIncludesDependencies()) {
                collectReachable(child, reachable);
            }
        }
    }

    /**
     * The coordinates without the version, which is settled by the mediation.
     */
    private static String coordinates(Dependency dependency) {
        return dependency.getGroupId()
                + ':'
                + dependency.getArtifactId()
                + ':'
                + dependency.getExtension()
                + ':'
                + dependency.getClassifier();
    }

    /**
     * @return the list of {@link #earModules}. This corresponds to modules needed at runtime.
     */
    protected List<EarModule> getModules() {
        if (earModules == null) {
            throw new IllegalStateException("Ear modules have not been initialized");
        }
        return earModules;
    }

    /**
     * @return the list of {@link #allEarModules}. This corresponds to all modules (provided + compile + runtime).
     */
    protected List<EarModule> getAllEarModules() {
        if (allEarModules == null) {
            throw new IllegalStateException("EAR modules have not been initialized");
        }
        return allEarModules;
    }

    /**
     * @return the list of {@link #providedEarModules}. This corresponds to provided modules.
     */
    protected List<EarModule> getProvidedEarModules() {
        if (providedEarModules == null) {
            throw new IllegalStateException("Jar modules have not been initialized");
        }
        return providedEarModules;
    }

    /**
     * @return the {@link Project}
     */
    protected Project getProject() {
        return project;
    }

    /**
     * @return the {@link Session}
     */
    protected Session getSession() {
        return session;
    }

    /**
     * Returns a configuration element that is a free form XML structure.
     * <p>
     * The element is read from the configuration of the mojo execution and not from the injected parameter: Maven 4
     * converts an {@link XmlNode} parameter without the XML attributes of the element, and those carry the
     * {@code artifactTypeMappings}, the {@code id} of the security roles and the class attributes of the JBoss
     * configuration.
     *
     * @param name the name of the parameter
     * @param injected the value injected by Maven, used if the execution has no configuration for the parameter
     * @return the configuration element, {@code null} if it is not configured
     */
    protected XmlConfig getConfiguration(String name, XmlNode injected) {
        final XmlNode configured = mojoExecution == null
                ? null
                : mojoExecution.getConfiguration().map(c -> c.child(name)).orElse(null);
        return XmlConfig.of(configured != null ? configured : injected);
    }

    /**
     * @return the mojo logger
     */
    protected Log getLog() {
        return log;
    }

    /**
     * @return {@link #workDirectory}
     */
    protected File getWorkDirectory() {
        return workDirectory;
    }

    /**
     * @return {@link #jbossConfiguration}
     */
    protected JbossConfiguration getJbossConfiguration() {
        return jbossConfiguration;
    }

    /**
     * @return {@link #outputFileNameMapping}
     */
    public String getOutputFileNameMapping() {
        return outputFileNameMapping;
    }

    private static boolean isArtifactRegistered(ResolvedArtifact artifact, List<EarModule> currentList) {
        return currentList.stream().anyMatch(em -> artifact.equals(em.getArtifact()));
    }

    /**
     * Initializes the JBoss configuration.
     *
     * @throws EarPluginException if the configuration is invalid
     */
    private void initializeJbossConfiguration() throws EarPluginException {
        final XmlConfig jboss = getConfiguration("jboss", this.jboss);
        if (jboss == null) {
            jbossConfiguration = null;
        } else {
            String childVersion = jboss.getChild(JbossConfiguration.VERSION).getValue();
            if (childVersion == null) {
                log.info("JBoss version not set, using JBoss 4 by default");
                childVersion = JbossConfiguration.VERSION_4;
            }
            final String securityDomain =
                    jboss.getChild(JbossConfiguration.SECURITY_DOMAIN).getValue();
            final String unauthenticatedPrincipal =
                    jboss.getChild(JbossConfiguration.UNAUHTHENTICTED_PRINCIPAL).getValue();

            final XmlConfig loaderRepositoryEl = jboss.getChild(JbossConfiguration.LOADER_REPOSITORY);
            final String loaderRepository = loaderRepositoryEl.getValue();
            final String loaderRepositoryClass =
                    loaderRepositoryEl.getAttribute(JbossConfiguration.LOADER_REPOSITORY_CLASS_ATTRIBUTE);
            final XmlConfig loaderRepositoryConfigEl = jboss.getChild(JbossConfiguration.LOADER_REPOSITORY_CONFIG);
            final String loaderRepositoryConfig = loaderRepositoryConfigEl.getValue();
            final String configParserClass =
                    loaderRepositoryConfigEl.getAttribute(JbossConfiguration.CONFIG_PARSER_CLASS_ATTRIBUTE);

            final String jmxName = jboss.getChild(JbossConfiguration.JMX_NAME).getValue();
            final String moduleOrder =
                    jboss.getChild(JbossConfiguration.MODULE_ORDER).getValue();

            final List<String> dataSources = new ArrayList<>();
            final XmlConfig dataSourcesEl = jboss.getChild(JbossConfiguration.DATASOURCES);
            if (dataSourcesEl != null) {

                final XmlConfig[] dataSourcesConfig = dataSourcesEl.getChildren(JbossConfiguration.DATASOURCE);
                for (XmlConfig dataSourceConfig : dataSourcesConfig) {
                    dataSources.add(dataSourceConfig.getValue());
                }
            }
            final String libraryDirectory =
                    jboss.getChild(JbossConfiguration.LIBRARY_DIRECTORY).getValue();
            jbossConfiguration = new JbossConfiguration(
                    childVersion,
                    securityDomain,
                    unauthenticatedPrincipal,
                    jmxName,
                    loaderRepository,
                    moduleOrder,
                    dataSources,
                    libraryDirectory,
                    loaderRepositoryConfig,
                    loaderRepositoryClass,
                    configParserClass);
        }
    }
}
