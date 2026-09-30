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

import java.util.List;

import org.apache.maven.api.xml.XmlNode;

/**
 * A read only view of a plugin configuration element. It offers the lookups of the Plexus configuration API the plugin
 * relied on, which a Maven 4 plugin cannot receive: a parameter of type {@code PlexusConfiguration} is not converted,
 * an {@link XmlNode} is.
 */
public final class XmlConfig {

    private static final XmlConfig[] NO_CHILDREN = new XmlConfig[0];

    private final XmlNode node;

    private XmlConfig(XmlNode node) {
        this.node = node;
    }

    /**
     * @param node the configuration element, can be {@code null}
     * @return the view of the element, or {@code null} if there is none
     */
    public static XmlConfig of(XmlNode node) {
        return node == null ? null : new XmlConfig(node);
    }

    /**
     * @return the element name
     */
    public String getName() {
        return node.name();
    }

    /**
     * @return the value of the element, or {@code null} if it has none
     */
    public String getValue() {
        return node.value();
    }

    /**
     * @param name the attribute name
     * @return the attribute value, or {@code null} if the attribute is not set
     */
    public String getAttribute(String name) {
        return node.attribute(name);
    }

    /**
     * @param name the child element name
     * @return the child element, an empty element if the child does not exist
     */
    public XmlConfig getChild(String name) {
        XmlNode child = node.child(name);
        return new XmlConfig(child != null ? child : XmlNode.newInstance(name));
    }

    /**
     * @param name the child element name
     * @return all the child elements with that name
     */
    public XmlConfig[] getChildren(String name) {
        List<XmlNode> children = node.children();
        if (children == null) {
            return NO_CHILDREN;
        }
        return children.stream()
                .filter(c -> name.equals(c.name()))
                .map(XmlConfig::new)
                .toArray(XmlConfig[]::new);
    }

    @Override
    public String toString() {
        return String.valueOf(node);
    }
}
