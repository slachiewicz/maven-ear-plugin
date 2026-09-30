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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.plugins.ear.EarModuleFactory;
import org.apache.maven.plugins.ear.EarPluginException;
import org.apache.maven.plugins.ear.UnknownArtifactTypeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the {@link ArtifactTypeMappingService}
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
class ArtifactTypeMappingServiceTest {

    @Test
    void testDefaultConfiguration() {
        ArtifactTypeMappingService service = getDefaultService();
        for (String type : EarModuleFactory.getStandardArtifactTypes()) {
            assertTrue(service.isMappedToType(type, type), "Standard type could not be found");
        }
    }

    @Test
    void testIsMappedToTypeForUnknownType() {
        ArtifactTypeMappingService service = getDefaultService();
        assertFalse(service.isMappedToType("rar", "MyKoolCustomType"));
    }

    @Test
    void testIsMappedToTypeForKnownType() {
        ArtifactTypeMappingService service = getServiceWithRarMappingToMyRar();
        assertTrue(service.isMappedToType("rar", "MyRar"));
    }

    @Test
    void testGetStandardTypeForUknonwnType() {
        try {
            ArtifactTypeMappingService service = getDefaultService();
            service.getStandardType("MyKoolCustomType");
            fail("Should have failed to retrieve a unknwon custom type");
        } catch (UnknownArtifactTypeException e) {
            // That's good
        }
    }

    @Test
    void testGetStandardTypeForKnownType() {
        try {
            ArtifactTypeMappingService service = getServiceWithRarMappingToMyRar();
            assertEquals("rar", service.getStandardType("MyRar"));
        } catch (UnknownArtifactTypeException e) {
            fail("Should not have failed to retrieve a knwon custom type " + e.getMessage());
        }
    }

    @Test
    void testConfigWithSameCustomType() {
        try {
            // the second mapping is left without attributes, as it always was
            XmlNode rootConfig = root("dummy", mapping("generic", "rar"), mapping(null, null));
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(rootConfig));
            fail("Should have failed");
        } catch (EarPluginException e) {
            // OK
        }
    }

    @Test
    void testConfigWithSameCustomTypeRegisteredTwice() {
        try {
            XmlNode rootConfig = root("dummy", mapping("generic", "rar"), mapping("generic", "ejb"));
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(rootConfig));
            fail("Should have failed");
        } catch (EarPluginException e) {
            // OK
        }
    }

    @Test
    void testConfigWithUnknownStandardType() {
        try {
            XmlNode rootConfig = root("dummy", mapping("generic", "notAStandardType"));
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(rootConfig));
            fail("Should have failed");
        } catch (EarPluginException e) {
            // OK
        }
    }

    @Test
    void testConfigWithNoType() {
        try {
            XmlNode rootConfig = root("dummy", mapping(null, "ejb"));
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(rootConfig));
            fail("Should have failed");
        } catch (EarPluginException e) {
            // OK
        }
    }

    @Test
    void testConfigWithNoMapping() {
        try {
            XmlNode rootConfig = root("dummy", mapping("generic", null));
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(rootConfig));
            fail("Should have failed");
        } catch (EarPluginException e) {
            // OK
        }
    }

    // Utilities

    private static XmlNode root(String name, XmlNode... children) {
        return XmlNode.newInstance(name, List.of(children));
    }

    private static XmlNode mapping(String type, String mapping) {
        Map<String, String> attributes = new HashMap<>();
        if (type != null) {
            attributes.put("type", type);
        }
        if (mapping != null) {
            attributes.put("mapping", mapping);
        }
        return XmlNode.newInstance(
                ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT, null, attributes, null, null);
    }

    protected ArtifactTypeMappingService getServiceWithRarMappingToMyRar() {
        try {
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(root("artifact-type-mappings", mapping("MyRar", "rar"))));

            return service;
        } catch (EarPluginException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // Won't occur
        return null;
    }

    protected ArtifactTypeMappingService getDefaultService() {
        try {
            ArtifactTypeMappingService service = new ArtifactTypeMappingService();
            service.configure(XmlConfig.of(root("dummy")));

            return service;
        } catch (EarPluginException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // Won't occur
        return null;
    }
}
