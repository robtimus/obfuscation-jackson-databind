/*
 * ObfuscatedBeanDeserializerModifierTest.java
 * Copyright 2020 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.obfuscation.jackson.databind;

import static com.github.robtimus.obfuscation.jackson.databind.ObfuscatedBeanDeserializerModifier.findClassSpecificObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class ObfuscatedBeanDeserializerModifierTest {

    @Nested
    @DisplayName("findClassSpecificObject(Class<?>, Map<Class<?>, T>, Map<Class<?>, T>)")
    class FindClassSpecificObject {

        @Test
        @DisplayName("direct match for interface")
        void testDirectMatchForInterface() {
            Map<Class<?>, String> classMappings = Collections.emptyMap();
            Map<Class<?>, String> interfaceMappings = Collections.singletonMap(ParentInterface.class, "match");
            String result = findClassSpecificObject(ParentInterface.class, classMappings, interfaceMappings);
            assertEquals("match", result);
        }

        @Test
        @DisplayName("direct match for class")
        void testDirectMatchForClass() {
            Map<Class<?>, String> classMappings = Collections.singletonMap(ParentClass.class, "match");
            Map<Class<?>, String> interfaceMappings = Collections.emptyMap();
            String result = findClassSpecificObject(ParentClass.class, classMappings, interfaceMappings);
            assertEquals("match", result);
        }

        @Test
        @DisplayName("direct match for super class")
        void testDirectMatchForSuperClass() {
            Map<Class<?>, String> classMappings = Collections.singletonMap(ParentClass.class, "match");
            Map<Class<?>, String> interfaceMappings = Collections.emptyMap();
            String result = findClassSpecificObject(SubClass.class, classMappings, interfaceMappings);
            assertEquals("match", result);
        }

        @Test
        @DisplayName("match for direct interface")
        void testMatchForDirectInterface() {
            Map<Class<?>, String> classMappings = Collections.emptyMap();
            Map<Class<?>, String> interfaceMappings = Collections.singletonMap(SubInterface.class, "match");
            String result = findClassSpecificObject(SubClass.class, classMappings, interfaceMappings);
            assertEquals("match", result);
        }

        @Test
        @DisplayName("match for interface super interface")
        void testMatchForInterfaceSuperInterface() {
            Map<Class<?>, String> classMappings = Collections.emptyMap();
            Map<Class<?>, String> interfaceMappings = Collections.singletonMap(ParentInterface.class, "match");
            String result = findClassSpecificObject(SubClass.class, classMappings, interfaceMappings);
            assertEquals("match", result);
        }

        @Test
        @DisplayName("match for super class interface")
        void testMatchForSuperClassInterface() {
            Map<Class<?>, String> classMappings = Collections.emptyMap();
            Map<Class<?>, String> interfaceMappings = Collections.singletonMap(Cloneable.class, "match");
            String result = findClassSpecificObject(SubClass.class, classMappings, interfaceMappings);
            assertEquals("match", result);
        }

        @Test
        @DisplayName("no match for interface")
        void testNoMatchForInterface() {
            Map<Class<?>, String> classMappings = Collections.emptyMap();
            Map<Class<?>, String> interfaceMappings = Collections.emptyMap();
            String result = findClassSpecificObject(ParentInterface.class, classMappings, interfaceMappings);
            assertNull(result);
        }

        @Test
        @DisplayName("no match for class")
        void testNoMatchForClass() {
            Map<Class<?>, String> classMappings = Collections.emptyMap();
            Map<Class<?>, String> interfaceMappings = Collections.emptyMap();
            String result = findClassSpecificObject(ParentClass.class, classMappings, interfaceMappings);
            assertNull(result);
        }
    }

    private interface ParentInterface {
        // no content
    }

    private interface SubInterface extends ParentInterface {
        // no content
    }

    private class ParentClass implements ParentInterface, Cloneable {
        // no content
    }

    private class SubClass extends ParentClass implements SubInterface {
        // no content
    }
}
