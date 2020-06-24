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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.ObfuscateFixedLength;
import com.github.robtimus.obfuscation.annotation.ObfuscateFixedValue;
import com.github.robtimus.obfuscation.annotation.ObfuscateNone;
import com.github.robtimus.obfuscation.annotation.ObfuscatePortion;
import com.github.robtimus.obfuscation.annotation.RepresentedBy;
import com.github.robtimus.obfuscation.annotation.StringRepresentationProvider.IntArrayToString;

@SuppressWarnings("nls")
class ObfuscationModuleTest {

    @Test
    @DisplayName("serialize")
    void testSerialize() throws IOException {
        Module module = ObfuscationModule.defaultModule();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(module);

        TestClass original = new TestClass();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, original);

        String json = writer.toString();
        assertThat(json, containsString("\"nullValue\":null"));
        assertThat(json, containsString("\"stringValue\":\"foo\""));
        assertThat(json, containsString("\"dateValue\":\"2020-05-07T12:30:55.123+0000\""));
        assertThat(json, containsString("\"intArray\":[1,2,3]"));
        assertThat(json, containsString("\"nestedClass\":{\"intValue\":13}"));
        assertThat(json, containsString("\"cws\":1"));
        assertThat(json, containsString("\"obfuscatedList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"regularList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"obfuscatedSet\":[\"foo\"]"));
        assertThat(json, containsString("\"regularSet\":[\"foo\"]"));
        assertThat(json, containsString("\"obfuscatedCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"regularCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"obfuscatedMap\":{\"1\":2}"));
        assertThat(json, containsString("\"regularMap\":{\"1\":2}"));
    }

    @Nested
    @DisplayName("deserialize")
    class Deserialize {

        @Test
        @DisplayName("with default module")
        void testWithDefaultModule() throws IOException {
            Module module = ObfuscationModule.defaultModule();

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(module);

            TestClass original = new TestClass();

            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, original);

            String json = writer.toString();

            TestClass deserialized = mapper.readValue(json, TestClass.class);

            assertEquals(original.stringValue, deserialized.stringValue);
            assertEquals(original.dateValue, deserialized.dateValue);
            assertNotNull(deserialized.intArray);
            assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
            assertNotNull(deserialized.nestedClass);
            assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
            assertNotNull(deserialized.classWithSerializer);
            assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);
            assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
            assertEquals(original.regularList, deserialized.regularList);
            assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
            assertEquals(original.regularSet, deserialized.regularSet);
            assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
            assertEquals(original.regularCollection, deserialized.regularCollection);
            assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
            assertEquals(original.regularMap, deserialized.regularMap);

            assertEquals("<string>", deserialized.stringValue.toString());
            assertEquals("***", deserialized.dateValue.toString());
            assertEquals("[***]", deserialized.intArray.toString());
            assertEquals("<<13>>", deserialized.nestedClass.toString());
            assertEquals("********", deserialized.classWithSerializer.toString());
            assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
            assertEquals("[foo, bar]", deserialized.regularList.toString());
            assertEquals("[********]", deserialized.obfuscatedSet.toString());
            assertEquals("[foo]", deserialized.regularSet.toString());
            assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
            assertEquals("[foo, bar]", deserialized.regularCollection.toString());
            assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
            assertEquals("{1=2}", deserialized.regularMap.toString());
        }

        @Test
        @DisplayName("with custom module")
        void testWithCustomModule() throws IOException {
            Module module = ObfuscationModule.builder()
                    .withDefaultObfuscator(Obfuscator.fixedValue("<default>"))
                    .build();

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(module);

            TestClass original = new TestClass();

            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, original);

            String json = writer.toString();

            TestClass deserialized = mapper.readValue(json, TestClass.class);

            assertEquals(original.stringValue, deserialized.stringValue);
            assertEquals(original.dateValue, deserialized.dateValue);
            assertNotNull(deserialized.intArray);
            assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
            assertNotNull(deserialized.nestedClass);
            assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
            assertNotNull(deserialized.classWithSerializer);
            assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);
            assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
            assertEquals(original.regularList, deserialized.regularList);
            assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
            assertEquals(original.regularSet, deserialized.regularSet);
            assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
            assertEquals(original.regularCollection, deserialized.regularCollection);
            assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
            assertEquals(original.regularMap, deserialized.regularMap);

            assertEquals("<string>", deserialized.stringValue.toString());
            assertEquals("<default>", deserialized.dateValue.toString());
            assertEquals("[***]", deserialized.intArray.toString());
            assertEquals("<<13>>", deserialized.nestedClass.toString());
            assertEquals("********", deserialized.classWithSerializer.toString());
            assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
            assertEquals("[foo, bar]", deserialized.regularList.toString());
            assertEquals("[********]", deserialized.obfuscatedSet.toString());
            assertEquals("[foo]", deserialized.regularSet.toString());
            assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
            assertEquals("[foo, bar]", deserialized.regularCollection.toString());
            assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
            assertEquals("{1=2}", deserialized.regularMap.toString());
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class TestClass {

        @ObfuscateFixedValue("<null>")
        private Obfuscated<String> nullValue = null;

        @ObfuscateFixedValue("<string>")
        private Obfuscated<String> stringValue = Obfuscator.all().obfuscateObject("foo");

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        @JsonSerialize(using = DateSerializer.class)
        @JsonDeserialize(using = DateDeserializer.class)
        private Obfuscated<Date> dateValue = Obfuscator.all().obfuscateObject(calculateDate());

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedLength = 3)
        @RepresentedBy(IntArrayToString.class)
        private Obfuscated<int[]> intArray = Obfuscator.all().obfuscateObject(new int[] { 1, 2, 3 });

        @ObfuscateNone
        @RepresentedBy(CustomStringRepresentationProvider.class)
        private Obfuscated<NestedClass> nestedClass = Obfuscator.all().obfuscateObject(new NestedClass());

        @ObfuscateFixedLength(8)
        @RepresentedBy(CustomStringRepresentationProvider.class)
        @JsonProperty("cws")
        private Obfuscated<ClassWithSerializer> classWithSerializer = Obfuscator.all().obfuscateObject(new ClassWithSerializer(1));

        @ObfuscateFixedLength(8)
        private List<String> obfuscatedList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        private List<String> regularList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscateFixedLength(8)
        private Set<String> obfuscatedSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        private Set<String> regularSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        @ObfuscateFixedLength(5)
        private Collection<String> obfuscatedCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        private Collection<String> regularCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscateFixedLength(6)
        private Map<Integer, Integer> obfuscatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

        private Map<Integer, Integer> regularMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

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
    static final class NestedClass {

        int intValue = 13;
    }

    @JsonSerialize(using = CustomSerializer.class)
    @JsonDeserialize(using = CustomDeserializer.class)
    private static final class ClassWithSerializer {

        private final int intValue;

        private ClassWithSerializer(int intValue) {
            this.intValue = intValue;
        }

        @Override
        public String toString() {
            return "{{" + intValue + "}}";
        }
    }

    private static final class CustomSerializer extends JsonSerializer<ClassWithSerializer> {

        @Override
        public void serialize(ClassWithSerializer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeNumber(value.intValue);
        }
    }

    private static final class CustomDeserializer extends JsonDeserializer<ClassWithSerializer> {

        @Override
        public ClassWithSerializer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            int intValue = p.getValueAsInt();
            return new ClassWithSerializer(intValue);
        }
    }
}
