/*
 * ObfuscationModuleTest.java
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;
import com.github.robtimus.obfuscation.annotation.ObfuscateAll;
import com.github.robtimus.obfuscation.annotation.ObfuscateFixedLength;
import com.github.robtimus.obfuscation.annotation.ObfuscateFixedValue;
import com.github.robtimus.obfuscation.annotation.ObfuscateNone;
import com.github.robtimus.obfuscation.annotation.ObfuscatePortion;
import com.github.robtimus.obfuscation.annotation.ObjectFactory;
import com.github.robtimus.obfuscation.annotation.RepresentedBy;
import tools.jackson.core.JacksonException.Reference;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.ClassUtil;

@SuppressWarnings({ "nls", "exports" })
class ObfuscationModuleTest {

    @Test
    @DisplayName("serialize")
    @SuppressWarnings("squid:S5961")
    void testSerialize() {
        JacksonModule module = ObfuscationModule.defaultModule();

        JsonMapper mapper = JsonMapper.builder()
                .addModule(module)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        TestClass original = new TestClass();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, original);

        String json = writer.toString();
        assertThat(json, containsString("\"nullValue\":null"));
        assertThat(json, containsString("\"fixedValue\":\"foo\""));
        assertThat(json, containsString("\"dateValue\":\"2020-05-07T12:30:55.123+0000\""));
        assertThat(json, containsString("\"intArray\":[1,2,3]"));
        assertThat(json, containsString("\"nestedClass\":{\"intValue\":13}"));
        assertThat(json, containsString("\"cws\":1"));
        assertThat(json, containsString("\"annotated\":{}"));
        assertThat(json, containsString("\"stringValue\":\"foo\""));
        assertThat(json, containsString("\"intValue\":1"));
        assertThat(json, containsString("\"obfuscatedList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"upperCaseObfuscatedList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"annotatedList\":[{}]"));
        assertThat(json, containsString("\"stringList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"intList\":[1,2]"));
        assertThat(json, containsString("\"obfuscatedSet\":[\"foo\"]"));
        assertThat(json, containsString("\"upperCaseObfuscatedSet\":[\"foo\"]"));
        assertThat(json, containsString("\"annotatedSet\":[{}]"));
        assertThat(json, containsString("\"stringSet\":[\"foo\"]"));
        assertThat(json, containsString("\"intSet\":[1]"));
        assertThat(json, containsString("\"obfuscatedCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"upperCaseObfuscatedCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"annotatedCollection\":[{}]"));
        assertThat(json, containsString("\"stringCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"intCollection\":[1,2]"));
        assertThat(json, containsString("\"obfuscatedMap\":{\"1\":2}"));
        assertThat(json, containsString("\"negateValueObfuscatedMap\":{\"1\":2}"));
        assertThat(json, containsString("\"annotatedMap\":{\"foo\":{}}"));
        assertThat(json, containsString("\"stringMap\":{\"foo\":\"bar\"}"));
        assertThat(json, containsString("\"intMap\":{\"1\":2}"));
        assertThat(json, containsString("\"obfuscatedDateList\":[1588854655123]"));
    }

    @Nested
    @DisplayName("deserialize")
    class Deserialize {

        @Test
        @DisplayName("with default module")
        @SuppressWarnings("squid:S5961")
        void testWithDefaultModule() {
            JacksonModule module = ObfuscationModule.defaultModule();

            JsonMapper mapper = JsonMapper.builder()
                    .addModule(module)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            TestClass original = new TestClass();

            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, original);

            String json = writer.toString();

            TestClass deserialized = mapper.readValue(json, TestClass.class);

            assertEquals(original.fixedValue, deserialized.fixedValue);
            assertEquals(original.dateValue, deserialized.dateValue);
            assertNotNull(deserialized.intArray);
            assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
            assertNotNull(deserialized.nestedClass);
            assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
            assertNotNull(deserialized.classWithSerializer);
            assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);
            assertEquals(original.annotated, deserialized.annotated);
            assertEquals(original.stringValue, deserialized.stringValue);
            assertEquals(original.intValue, deserialized.intValue);
            assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
            assertEquals(original.upperCaseObfuscatedList, deserialized.upperCaseObfuscatedList);
            assertEquals(original.annotatedList, deserialized.annotatedList);
            assertEquals(original.stringList, deserialized.stringList);
            assertEquals(original.intList, deserialized.intList);
            assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
            assertEquals(original.upperCaseObfuscatedSet, deserialized.upperCaseObfuscatedSet);
            assertEquals(original.annotatedSet, deserialized.annotatedSet);
            assertEquals(original.stringSet, deserialized.stringSet);
            assertEquals(original.intSet, deserialized.intSet);
            assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
            assertEquals(original.upperCaseObfuscatedCollection, deserialized.upperCaseObfuscatedCollection);
            assertEquals(original.annotatedCollection, deserialized.annotatedCollection);
            assertEquals(original.stringCollection, deserialized.stringCollection);
            assertEquals(original.intCollection, deserialized.intCollection);
            assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
            assertEquals(original.negateValueObfuscatedMap, deserialized.negateValueObfuscatedMap);
            assertEquals(original.annotatedMap, deserialized.annotatedMap);
            assertEquals(original.stringMap, deserialized.stringMap);
            assertEquals(original.intMap, deserialized.intMap);
            assertEquals(toLocalDates(original.obfuscatedDateList), toLocalDates(deserialized.obfuscatedDateList));

            assertEquals("<string>", deserialized.fixedValue.toString());
            assertEquals("***", deserialized.dateValue.toString());
            assertEquals("[***]", deserialized.intArray.toString());
            assertEquals("<<13>>", deserialized.nestedClass.toString());
            assertEquals("********", deserialized.classWithSerializer.toString());
            assertEquals("an**ed", deserialized.annotated.toString());
            assertEquals("***", deserialized.stringValue.toString());
            assertEquals("***", deserialized.intValue.toString());
            assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
            assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedList.toString());
            assertEquals("[an**ed]", deserialized.annotatedList.toString());
            assertEquals("[foo, bar]", deserialized.stringList.toString());
            assertEquals("[1, 2]", deserialized.intList.toString());
            assertEquals("[********]", deserialized.obfuscatedSet.toString());
            assertEquals("[F***O]", deserialized.upperCaseObfuscatedSet.toString());
            assertEquals("[an**ed]", deserialized.annotatedSet.toString());
            assertEquals("[foo]", deserialized.stringSet.toString());
            assertEquals("[1]", deserialized.intSet.toString());
            assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
            assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedCollection.toString());
            assertEquals("[an**ed]", deserialized.annotatedCollection.toString());
            assertEquals("[foo, bar]", deserialized.stringCollection.toString());
            assertEquals("[1, 2]", deserialized.intCollection.toString());
            assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
            assertEquals("{1=-***2}", deserialized.negateValueObfuscatedMap.toString());
            assertEquals("{foo=an**ed}", deserialized.annotatedMap.toString());
            assertEquals("{foo=bar}", deserialized.stringMap.toString());
            assertEquals("{1=2}", deserialized.intMap.toString());
            assertEquals("[2020-05-**]", deserialized.obfuscatedDateList.toString());
        }

        @Nested
        @DisplayName("with custom module")
        class WithCustomModule {

            @Test
            @DisplayName("with custom default obfuscator")
            @SuppressWarnings("squid:S5961")
            void testWithCustomDefaultObfuscator() {
                JacksonModule module = ObfuscationModule.builder()
                        .withDefaultObfuscator(Obfuscator.fixedValue("<default>"))
                        .withDefaultObfuscator(Number.class, Obfuscator.portion().keepAtStart(2).keepAtEnd(2).withFixedTotalLength(6).build())
                        .withDefaultCharacterRepresentation(Number.class, s -> "<number>")
                        .withDefaultObfuscator(CharSequence.class, Obfuscator.portion().keepAtStart(2).keepAtEnd(2).withFixedTotalLength(6).build())
                        .withDefaultCharacterRepresentation(CharSequence.class, s -> "<charSequence>")
                        // not used, just to show that the maps are not overwritten
                        .withDefaultObfuscator(File.class, Obfuscator.none())
                        .withDefaultCharacterRepresentation(File.class, s -> "file")
                        .withDefaultObfuscator(Path.class, Obfuscator.none())
                        .withDefaultCharacterRepresentation(Path.class, s -> "path")
                        .build();

                JsonMapper mapper = JsonMapper.builder()
                        .addModule(module)
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();

                TestClass original = new TestClass();

                StringWriter writer = new StringWriter();
                mapper.writeValue(writer, original);

                String json = writer.toString();

                TestClass deserialized = mapper.readValue(json, TestClass.class);

                assertEquals(original.fixedValue, deserialized.fixedValue);
                assertEquals(original.dateValue, deserialized.dateValue);
                assertNotNull(deserialized.intArray);
                assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
                assertNotNull(deserialized.nestedClass);
                assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
                assertNotNull(deserialized.classWithSerializer);
                assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);
                assertEquals(original.annotated, deserialized.annotated);
                assertEquals(original.stringValue, deserialized.stringValue);
                assertEquals(original.intValue, deserialized.intValue);
                assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
                assertEquals(original.upperCaseObfuscatedList, deserialized.upperCaseObfuscatedList);
                assertEquals(original.annotatedList, deserialized.annotatedList);
                assertEquals(original.stringList, deserialized.stringList);
                assertEquals(original.intList, deserialized.intList);
                assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
                assertEquals(original.upperCaseObfuscatedSet, deserialized.upperCaseObfuscatedSet);
                assertEquals(original.annotatedSet, deserialized.annotatedSet);
                assertEquals(original.stringSet, deserialized.stringSet);
                assertEquals(original.intSet, deserialized.intSet);
                assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
                assertEquals(original.upperCaseObfuscatedCollection, deserialized.upperCaseObfuscatedCollection);
                assertEquals(original.annotatedCollection, deserialized.annotatedCollection);
                assertEquals(original.stringCollection, deserialized.stringCollection);
                assertEquals(original.intCollection, deserialized.intCollection);
                assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
                assertEquals(original.negateValueObfuscatedMap, deserialized.negateValueObfuscatedMap);
                assertEquals(original.annotatedMap, deserialized.annotatedMap);
                assertEquals(original.stringMap, deserialized.stringMap);
                assertEquals(original.intMap, deserialized.intMap);
                assertEquals(toLocalDates(original.obfuscatedDateList), toLocalDates(deserialized.obfuscatedDateList));

                assertEquals("<string>", deserialized.fixedValue.toString());
                assertEquals("<default>", deserialized.dateValue.toString());
                assertEquals("[***]", deserialized.intArray.toString());
                assertEquals("<<13>>", deserialized.nestedClass.toString());
                assertEquals("********", deserialized.classWithSerializer.toString());
                assertEquals("an**ed", deserialized.annotated.toString());
                assertEquals("<c**e>", deserialized.stringValue.toString());
                assertEquals("<n**r>", deserialized.intValue.toString());
                assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedList.toString());
                assertEquals("[an**ed]", deserialized.annotatedList.toString());
                assertEquals("[<c**e>, <c**e>]", deserialized.stringList.toString());
                assertEquals("[<n**r>, <n**r>]", deserialized.intList.toString());
                assertEquals("[********]", deserialized.obfuscatedSet.toString());
                assertEquals("[F***O]", deserialized.upperCaseObfuscatedSet.toString());
                assertEquals("[an**ed]", deserialized.annotatedSet.toString());
                assertEquals("[<c**e>]", deserialized.stringSet.toString());
                assertEquals("[<n**r>]", deserialized.intSet.toString());
                assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedCollection.toString());
                assertEquals("[an**ed]", deserialized.annotatedCollection.toString());
                assertEquals("[<c**e>, <c**e>]", deserialized.stringCollection.toString());
                assertEquals("[<n**r>, <n**r>]", deserialized.intCollection.toString());
                assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
                assertEquals("{1=-***2}", deserialized.negateValueObfuscatedMap.toString());
                assertEquals("{foo=an**ed}", deserialized.annotatedMap.toString());
                assertEquals("{foo=<c**e>}", deserialized.stringMap.toString());
                assertEquals("{1=<n**r>}", deserialized.intMap.toString());
                assertEquals("[2020-05-**]", deserialized.obfuscatedDateList.toString());
            }

            @Test
            @DisplayName("with custom ObjectFactory")
            @SuppressWarnings("squid:S5961")
            void testCustomObjectFactory() {
                ObjectFactory objectFactory = spy(new ObjectFactory() {

                    @Override
                    public <T> T instance(Class<T> type) {
                        return ClassUtil.createInstance(type, true);
                    }
                });

                JacksonModule module = ObfuscationModule.builder()
                        .withDefaultObfuscator(Number.class, Obfuscator.portion().keepAtStart(2).keepAtEnd(2).withFixedTotalLength(6).build())
                        .withDefaultCharacterRepresentation(Number.class, s -> "<number>")
                        .withDefaultObfuscator(CharSequence.class, Obfuscator.portion().keepAtStart(2).keepAtEnd(2).withFixedTotalLength(6).build())
                        .withDefaultCharacterRepresentation(CharSequence.class, s -> "<charSequence>")
                        // not used, just to show that the maps are not overwritten
                        .withDefaultObfuscator(File.class, Obfuscator.none())
                        .withDefaultCharacterRepresentation(File.class, s -> "file")
                        .withDefaultObfuscator(Path.class, Obfuscator.none())
                        .withDefaultCharacterRepresentation(Path.class, s -> "path")
                        .withObjectFactory(objectFactory)
                        .build();

                JsonMapper mapper = JsonMapper.builder()
                        .addModule(module)
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();

                TestClass original = new TestClass();

                StringWriter writer = new StringWriter();
                mapper.writeValue(writer, original);

                String json = writer.toString();

                TestClass deserialized = mapper.readValue(json, TestClass.class);

                assertEquals(original.fixedValue, deserialized.fixedValue);
                assertEquals(original.dateValue, deserialized.dateValue);
                assertNotNull(deserialized.intArray);
                assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
                assertNotNull(deserialized.nestedClass);
                assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
                assertNotNull(deserialized.classWithSerializer);
                assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);
                assertEquals(original.annotated, deserialized.annotated);
                assertEquals(original.stringValue, deserialized.stringValue);
                assertEquals(original.intValue, deserialized.intValue);
                assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
                assertEquals(original.upperCaseObfuscatedList, deserialized.upperCaseObfuscatedList);
                assertEquals(original.annotatedList, deserialized.annotatedList);
                assertEquals(original.stringList, deserialized.stringList);
                assertEquals(original.intList, deserialized.intList);
                assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
                assertEquals(original.upperCaseObfuscatedSet, deserialized.upperCaseObfuscatedSet);
                assertEquals(original.annotatedSet, deserialized.annotatedSet);
                assertEquals(original.stringSet, deserialized.stringSet);
                assertEquals(original.intSet, deserialized.intSet);
                assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
                assertEquals(original.upperCaseObfuscatedCollection, deserialized.upperCaseObfuscatedCollection);
                assertEquals(original.annotatedCollection, deserialized.annotatedCollection);
                assertEquals(original.stringCollection, deserialized.stringCollection);
                assertEquals(original.intCollection, deserialized.intCollection);
                assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
                assertEquals(original.negateValueObfuscatedMap, deserialized.negateValueObfuscatedMap);
                assertEquals(original.annotatedMap, deserialized.annotatedMap);
                assertEquals(original.stringMap, deserialized.stringMap);
                assertEquals(original.intMap, deserialized.intMap);
                assertEquals(toLocalDates(original.obfuscatedDateList), toLocalDates(deserialized.obfuscatedDateList));

                assertEquals("<string>", deserialized.fixedValue.toString());
                assertEquals("***", deserialized.dateValue.toString());
                assertEquals("[***]", deserialized.intArray.toString());
                assertEquals("<<13>>", deserialized.nestedClass.toString());
                assertEquals("********", deserialized.classWithSerializer.toString());
                assertEquals("an**ed", deserialized.annotated.toString());
                assertEquals("<c**e>", deserialized.stringValue.toString());
                assertEquals("<n**r>", deserialized.intValue.toString());
                assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedList.toString());
                assertEquals("[an**ed]", deserialized.annotatedList.toString());
                assertEquals("[<c**e>, <c**e>]", deserialized.stringList.toString());
                assertEquals("[<n**r>, <n**r>]", deserialized.intList.toString());
                assertEquals("[********]", deserialized.obfuscatedSet.toString());
                assertEquals("[F***O]", deserialized.upperCaseObfuscatedSet.toString());
                assertEquals("[an**ed]", deserialized.annotatedSet.toString());
                assertEquals("[<c**e>]", deserialized.stringSet.toString());
                assertEquals("[<n**r>]", deserialized.intSet.toString());
                assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedCollection.toString());
                assertEquals("[an**ed]", deserialized.annotatedCollection.toString());
                assertEquals("[<c**e>, <c**e>]", deserialized.stringCollection.toString());
                assertEquals("[<n**r>, <n**r>]", deserialized.intCollection.toString());
                assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
                assertEquals("{1=-***2}", deserialized.negateValueObfuscatedMap.toString());
                assertEquals("{foo=an**ed}", deserialized.annotatedMap.toString());
                assertEquals("{foo=<c**e>}", deserialized.stringMap.toString());
                assertEquals("{1=<n**r>}", deserialized.intMap.toString());
                assertEquals("[2020-05-**]", deserialized.obfuscatedDateList.toString());

                ArgumentCaptor<Class<?>> typeCaptor = ArgumentCaptor.forClass(Class.class);

                verify(objectFactory, atLeastOnce()).instance(typeCaptor.capture());

                Set<Class<?>> types = new HashSet<>(typeCaptor.getAllValues());
                Set<Class<?>> expectedTypes = new HashSet<>(Arrays.asList(
                        CustomCharacterRepresentationProvider.class,
                        DateFormat.class,
                        NegateValueToString.class,
                        UpperCase.class,
                        AnnotatedRepresentation.class
                ));
                assertEquals(expectedTypes, types);
            }

            @Test
            @DisplayName("without access modifier fix")
            void testWithoutAccessModifierFix() {
                JacksonModule module = ObfuscationModule.defaultModule();

                JsonMapper mapper = JsonMapper.builder()
                        .addModule(module)
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                        .build();

                int originalInstantiationCount = CustomCharacterRepresentationProvider.getInstantiationCount();

                TestClass original = new TestClass();

                StringWriter writer = new StringWriter();
                assertDoesNotThrow(() -> JsonMapper.builder()
                        .addModule(module)
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build()
                        .writeValue(writer, original));

                String json = writer.toString();

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> mapper.readValue(json, TestClass.class));
                assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
                assertEquals(getExpectedErrorMessage(UpperCase.class), exception.getCause().getMessage());

                // 2 additional instantiations: nestedClass and classWithSerializer
                assertEquals(originalInstantiationCount + 2, CustomCharacterRepresentationProvider.getInstantiationCount());
            }

            private String getExpectedErrorMessage(Class<?> cls) {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ClassUtil.createInstance(cls, false));
                return exception.getMessage();
            }

            @Test
            @DisplayName("requiring annotated obfuscator")
            @SuppressWarnings("squid:S5961")
            void testRequiringAnnotatedObfuscator() {
                JacksonModule module = ObfuscationModule.builder()
                        .withDefaultObfuscator(Obfuscator.fixedValue("<default>"))
                        .withDefaultObfuscator(Number.class, Obfuscator.portion().keepAtStart(2).keepAtEnd(2).withFixedTotalLength(6).build())
                        .withDefaultCharacterRepresentation(Number.class, s -> "<number>")
                        .withDefaultObfuscator(CharSequence.class, Obfuscator.portion().keepAtStart(2).keepAtEnd(2).withFixedTotalLength(6).build())
                        .withDefaultCharacterRepresentation(CharSequence.class, s -> "<charSequence>")
                        // not used, just to show that the maps are not overwritten
                        .withDefaultObfuscator(File.class, Obfuscator.none())
                        .withDefaultCharacterRepresentation(File.class, s -> "file")
                        .withDefaultObfuscator(Path.class, Obfuscator.none())
                        .withDefaultCharacterRepresentation(Path.class, s -> "path")
                        .requireObfuscatorAnnotation(true)
                        .build();

                JsonMapper mapper = JsonMapper.builder()
                        .addModule(module)
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();

                TestClass original = new TestClass();

                StringWriter writer = new StringWriter();
                mapper.writeValue(writer, original);

                String json = writer.toString();

                TestClass deserialized = mapper.readValue(json, TestClass.class);

                assertEquals(original.fixedValue, deserialized.fixedValue);
                assertEquals(original.dateValue, deserialized.dateValue);
                assertNotNull(deserialized.intArray);
                assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
                assertNotNull(deserialized.nestedClass);
                assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
                assertNotNull(deserialized.classWithSerializer);
                assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);
                assertEquals(original.annotated, deserialized.annotated);
                assertEquals(original.stringValue, deserialized.stringValue);
                assertEquals(original.intValue, deserialized.intValue);
                assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
                assertEquals(original.upperCaseObfuscatedList, deserialized.upperCaseObfuscatedList);
                assertEquals(original.annotatedList, deserialized.annotatedList);
                assertEquals(original.stringList, deserialized.stringList);
                assertEquals(original.intList, deserialized.intList);
                assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
                assertEquals(original.upperCaseObfuscatedSet, deserialized.upperCaseObfuscatedSet);
                assertEquals(original.annotatedSet, deserialized.annotatedSet);
                assertEquals(original.stringSet, deserialized.stringSet);
                assertEquals(original.intSet, deserialized.intSet);
                assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
                assertEquals(original.upperCaseObfuscatedCollection, deserialized.upperCaseObfuscatedCollection);
                assertEquals(original.annotatedCollection, deserialized.annotatedCollection);
                assertEquals(original.stringCollection, deserialized.stringCollection);
                assertEquals(original.intCollection, deserialized.intCollection);
                assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
                assertEquals(original.negateValueObfuscatedMap, deserialized.negateValueObfuscatedMap);
                assertEquals(original.annotatedMap, deserialized.annotatedMap);
                assertEquals(original.stringMap, deserialized.stringMap);
                assertEquals(original.intMap, deserialized.intMap);
                assertEquals(toLocalDates(original.obfuscatedDateList), toLocalDates(deserialized.obfuscatedDateList));

                assertEquals("<string>", deserialized.fixedValue.toString());
                assertEquals("<default>", deserialized.dateValue.toString());
                assertEquals("[***]", deserialized.intArray.toString());
                assertEquals("<<13>>", deserialized.nestedClass.toString());
                assertEquals("********", deserialized.classWithSerializer.toString());
                assertEquals("an**ed", deserialized.annotated.toString());
                assertEquals("<c**e>", deserialized.stringValue.toString());
                assertEquals("<n**r>", deserialized.intValue.toString());
                assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedList.toString());
                assertEquals("[" + deserialized.annotatedList.iterator().next() + "]", deserialized.annotatedList.toString());
                assertEquals("[foo, bar]", deserialized.stringList.toString());
                assertEquals("[1, 2]", deserialized.intList.toString());
                assertEquals("[********]", deserialized.obfuscatedSet.toString());
                assertEquals("[F***O]", deserialized.upperCaseObfuscatedSet.toString());
                assertEquals("[" + deserialized.annotatedSet.iterator().next() + "]", deserialized.annotatedSet.toString());
                assertEquals("[foo]", deserialized.stringSet.toString());
                assertEquals("[1]", deserialized.intSet.toString());
                assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedCollection.toString());
                assertEquals("[" + deserialized.annotatedCollection.iterator().next() + "]", deserialized.annotatedCollection.toString());
                assertEquals("[foo, bar]", deserialized.stringCollection.toString());
                assertEquals("[1, 2]", deserialized.intCollection.toString());
                assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
                assertEquals("{1=-***2}", deserialized.negateValueObfuscatedMap.toString());
                assertEquals("{foo=" + deserialized.annotatedMap.get("foo") + "}", deserialized.annotatedMap.toString());
                assertEquals("{foo=bar}", deserialized.stringMap.toString());
                assertEquals("{1=2}", deserialized.intMap.toString());
                assertEquals("[2020-05-**]", deserialized.obfuscatedDateList.toString());
            }
        }

        @Test
        @DisplayName("with non-deserializable type")
        void testWithNonDeserializableType() {
            JacksonModule module = ObfuscationModule.defaultModule();

            JsonMapper mapper = JsonMapper.builder()
                    .addModule(module)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                    .build();

            WithNonDeserializableType original = new WithNonDeserializableType();

            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, original);

            String json = writer.toString();

            InvalidDefinitionException exception = assertThrows(InvalidDefinitionException.class,
                    () -> mapper.readValue(json, WithNonDeserializableType.class));

            // the exception should refer to the generic type, not Obfuscated

            assertEquals(mapper.getTypeFactory().constructType(Runnable.class), exception.getType());

            List<Reference> path = exception.getPath();
            assertThat(path, hasSize(1));

            Reference reference = path.get(0);
            assertEquals(WithNonDeserializableType.class.getName() + "[\"value\"]", reference.getDescription());
            assertEquals("value", reference.getPropertyName());
        }

        private List<LocalDate> toLocalDates(List<Date> dates) {
            return dates.stream()
                    .map(this::toLocalDate)
                    .toList();
        }

        private LocalDate toLocalDate(Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    @Nested
    @DisplayName("using constructor")
    class UsingConstructorTest {

        @Test
        @DisplayName("serialize")
        void testSerialize() {
            JacksonModule module = ObfuscationModule.defaultModule();

            JsonMapper mapper = JsonMapper.builder()
                    .addModule(module)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            Obfuscated<String> value = Obfuscator.fixedLength(8).obfuscateObject("foobar");
            List<String> list = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));
            UsingConstructor original = new UsingConstructor(value, list);

            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, original);

            String json = writer.toString();
            assertThat(json, containsString("\"value\":\"foobar\""));
        }

        @Test
        @DisplayName("deserialize")
        void testDeserialize() {
            JacksonModule module = ObfuscationModule.defaultModule();

            JsonMapper mapper = JsonMapper.builder()
                    .addModule(module)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            Obfuscated<String> value = Obfuscator.fixedLength(8).obfuscateObject("foobar");
            List<String> list = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));
            UsingConstructor original = new UsingConstructor(value, list);

            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, original);

            String json = writer.toString();

            UsingConstructor deserialized = mapper.readValue(json, UsingConstructor.class);

            assertEquals(original.value, deserialized.value);

            assertEquals("********", deserialized.value.toString());
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class TestClass {

        @ObfuscateFixedValue("<null>")
        public Obfuscated<String> nullValue = null;

        @ObfuscateFixedValue("<string>")
        public Obfuscated<String> fixedValue = Obfuscator.all().obfuscateObject("foo");

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        public Obfuscated<Date> dateValue = Obfuscator.all().obfuscateObject(calculateDate());

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        public Obfuscated<int[]> intArray = Obfuscator.all().obfuscateObject(new int[] { 1, 2, 3 });

        @ObfuscateNone
        @RepresentedBy(CustomCharacterRepresentationProvider.class)
        public Obfuscated<NestedClass> nestedClass = Obfuscator.all().obfuscateObject(new NestedClass());

        @ObfuscateFixedLength(8)
        @RepresentedBy(CustomCharacterRepresentationProvider.class)
        @JsonProperty("cws")
        public Obfuscated<ClassWithSerializer> classWithSerializer = Obfuscator.all().obfuscateObject(new ClassWithSerializer(1));

        public Obfuscated<Annotated> annotated = Obfuscator.all().obfuscateObject(new Annotated());

        public Obfuscated<String> stringValue = Obfuscator.all().obfuscateObject("foo");
        public Obfuscated<Integer> intValue = Obfuscator.all().obfuscateObject(1);

        @ObfuscateFixedLength(8)
        public List<String> obfuscatedList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(UpperCase.class)
        public List<String> upperCaseObfuscatedList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        public List<Annotated> annotatedList = Obfuscator.all().obfuscateList(Arrays.asList(new Annotated()));

        public List<String> stringList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));
        public List<Integer> intList = Obfuscator.all().obfuscateList(Arrays.asList(1, 2));

        @ObfuscateFixedLength(8)
        public Set<String> obfuscatedSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(UpperCase.class)
        public Set<String> upperCaseObfuscatedSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        public Set<Annotated> annotatedSet = Obfuscator.all().obfuscateSet(Collections.singleton(new Annotated()));

        public Set<String> stringSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));
        public Set<Integer> intSet = Obfuscator.all().obfuscateSet(Collections.singleton(1));

        @ObfuscateFixedLength(5)
        public Collection<String> obfuscatedCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(UpperCase.class)
        public Collection<String> upperCaseObfuscatedCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        public Collection<Annotated> annotatedCollection = Obfuscator.all().obfuscateCollection(Arrays.asList(new Annotated()));
        public Collection<Integer> intCollection = Obfuscator.all().obfuscateCollection(Arrays.asList(1, 2));

        public Collection<String> stringCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscateFixedLength(6)
        public Map<Integer, Integer> obfuscatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(NegateValueToString.class)
        public Map<Integer, Integer> negateValueObfuscatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

        public Map<String, Annotated> annotatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap("foo", new Annotated()));

        public Map<String, String> stringMap = Obfuscator.all().obfuscateMap(Collections.singletonMap("foo", "bar"));
        public Map<Integer, Integer> intMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

        @ObfuscatePortion(keepAtStart = 8)
        @RepresentedBy(DateFormat.class)
        public List<Date> obfuscatedDateList = Obfuscator.all().obfuscateList(Arrays.asList(calculateDate()));

        private Date calculateDate() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, 2020);
            calendar.set(Calendar.MONTH, Calendar.MAY);
            calendar.set(Calendar.DAY_OF_MONTH, 7);
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 30);
            calendar.set(Calendar.SECOND, 55);
            calendar.set(Calendar.MILLISECOND, 123);
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            return calendar.getTime();
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class NestedClass {

        public int intValue = 13;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    @ObfuscatePortion(keepAtStart = 2, keepAtEnd = 2, fixedTotalLength = 6)
    @RepresentedBy(AnnotatedRepresentation.class)
    public static final class Annotated {
        // no content necessary

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Annotated;
        }

        @Override
        public int hashCode() {
            return Annotated.class.hashCode();
        }
    }

    public static final class AnnotatedRepresentation implements CharacterRepresentationProvider {

        @Override
        public CharSequence toCharSequence(Object value) {
            return "annotated";
        }
    }

    @JsonSerialize(using = CustomSerializer.class)
    @JsonDeserialize(using = CustomDeserializer.class)
    public static final class ClassWithSerializer {

        private final int intValue;

        private ClassWithSerializer(int intValue) {
            this.intValue = intValue;
        }

        @Override
        public String toString() {
            return "{{" + intValue + "}}";
        }
    }

    public static final class UsingConstructor {

        private final Obfuscated<String> value;
        private final List<String> list;

        @JsonCreator
        UsingConstructor(
                @JsonProperty("value") @ObfuscateFixedLength(8) Obfuscated<String> value,
                @JsonProperty("list") @ObfuscateAll List<String> list) {

            this.value = value;
            this.list = list;
        }

        public Obfuscated<String> getValue() {
            return value;
        }

        public List<String> getList() {
            return list;
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class WithNonDeserializableType {

        public final Obfuscated<Runnable> value = Obfuscator.all().obfuscateObject(() -> { /* do nothing */ });
    }

    public static final class CustomSerializer extends ValueSerializer<ClassWithSerializer> {

        @Override
        @SuppressWarnings("resource")
        public void serialize(ClassWithSerializer value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeNumber(value.intValue);
        }
    }

    public static final class CustomDeserializer extends ValueDeserializer<ClassWithSerializer> {

        @Override
        public ClassWithSerializer deserialize(JsonParser p, DeserializationContext ctxt) {
            int intValue = p.getValueAsInt();
            return new ClassWithSerializer(intValue);
        }
    }
}
