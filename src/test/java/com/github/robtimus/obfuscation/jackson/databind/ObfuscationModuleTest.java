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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.IntArrayToString;
import com.github.robtimus.obfuscation.annotation.ObfuscateFixedLength;
import com.github.robtimus.obfuscation.annotation.ObfuscateFixedValue;
import com.github.robtimus.obfuscation.annotation.ObfuscateNone;
import com.github.robtimus.obfuscation.annotation.ObfuscatePortion;
import com.github.robtimus.obfuscation.annotation.RepresentedBy;

@SuppressWarnings("nls")
class ObfuscationModuleTest {

    @Test
    @DisplayName("serialize")
    void testSerialize() throws IOException {
        Module module = ObfuscationModule.defaultModule();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(module)
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
        assertThat(json, containsString("\"annotated\":{}"));
        assertThat(json, containsString("\"obfuscatedList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"upperCaseObfuscatedList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"annotatedList\":[{}]"));
        assertThat(json, containsString("\"regularList\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"obfuscatedSet\":[\"foo\"]"));
        assertThat(json, containsString("\"upperCaseObfuscatedSet\":[\"foo\"]"));
        assertThat(json, containsString("\"annotatedSet\":[{}]"));
        assertThat(json, containsString("\"regularSet\":[\"foo\"]"));
        assertThat(json, containsString("\"obfuscatedCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"upperCaseObfuscatedCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"annotatedCollection\":[{}]"));
        assertThat(json, containsString("\"regularCollection\":[\"foo\",\"bar\"]"));
        assertThat(json, containsString("\"obfuscatedMap\":{\"1\":2}"));
        assertThat(json, containsString("\"negateValueObfuscatedMap\":{\"1\":2}"));
        assertThat(json, containsString("\"annotatedMap\":{\"foo\":{}}"));
        assertThat(json, containsString("\"regularMap\":{\"1\":2}"));
        assertThat(json, containsString("\"obfuscatedDateList\":[1588854655123]"));
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
            assertEquals(original.annotated, deserialized.annotated);
            assertEquals(original.obfuscatedList, deserialized.obfuscatedList);
            assertEquals(original.upperCaseObfuscatedList, deserialized.upperCaseObfuscatedList);
            assertEquals(original.annotatedList, deserialized.annotatedList);
            assertEquals(original.regularList, deserialized.regularList);
            assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
            assertEquals(original.upperCaseObfuscatedSet, deserialized.upperCaseObfuscatedSet);
            assertEquals(original.annotatedSet, deserialized.annotatedSet);
            assertEquals(original.regularSet, deserialized.regularSet);
            assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
            assertEquals(original.upperCaseObfuscatedCollection, deserialized.upperCaseObfuscatedCollection);
            assertEquals(original.annotatedCollection, deserialized.annotatedCollection);
            assertEquals(original.regularCollection, deserialized.regularCollection);
            assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
            assertEquals(original.negateValueObfuscatedMap, deserialized.negateValueObfuscatedMap);
            assertEquals(original.annotatedMap, deserialized.annotatedMap);
            assertEquals(original.regularMap, deserialized.regularMap);
            assertEquals(toLocalDates(original.obfuscatedDateList), toLocalDates(deserialized.obfuscatedDateList));

            assertEquals("<string>", deserialized.stringValue.toString());
            assertEquals("***", deserialized.dateValue.toString());
            assertEquals("[***]", deserialized.intArray.toString());
            assertEquals("<<13>>", deserialized.nestedClass.toString());
            assertEquals("********", deserialized.classWithSerializer.toString());
            assertEquals("an**ed", deserialized.annotated.toString());
            assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
            assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedList.toString());
            assertEquals("[an**ed]", deserialized.annotatedList.toString());
            assertEquals("[foo, bar]", deserialized.regularList.toString());
            assertEquals("[********]", deserialized.obfuscatedSet.toString());
            assertEquals("[F***O]", deserialized.upperCaseObfuscatedSet.toString());
            assertEquals("[an**ed]", deserialized.annotatedSet.toString());
            assertEquals("[foo]", deserialized.regularSet.toString());
            assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
            assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedCollection.toString());
            assertEquals("[an**ed]", deserialized.annotatedCollection.toString());
            assertEquals("[foo, bar]", deserialized.regularCollection.toString());
            assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
            assertEquals("{1=-***2}", deserialized.negateValueObfuscatedMap.toString());
            assertEquals("{foo=an**ed}", deserialized.annotatedMap.toString());
            assertEquals("{1=2}", deserialized.regularMap.toString());
            assertEquals("[2020-05-**]", deserialized.obfuscatedDateList.toString());
        }

        @Nested
        @DisplayName("with custom module")
        class WithCustomModule {

            @Test
            @DisplayName("with custom default obfuscator")
            void testWithCustomDefaultObfuscator() throws IOException {
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
                assertEquals(original.upperCaseObfuscatedList, deserialized.upperCaseObfuscatedList);
                assertEquals(original.regularList, deserialized.regularList);
                assertEquals(original.obfuscatedSet, deserialized.obfuscatedSet);
                assertEquals(original.upperCaseObfuscatedSet, deserialized.upperCaseObfuscatedSet);
                assertEquals(original.regularSet, deserialized.regularSet);
                assertEquals(original.obfuscatedCollection, deserialized.obfuscatedCollection);
                assertEquals(original.upperCaseObfuscatedCollection, deserialized.upperCaseObfuscatedCollection);
                assertEquals(original.regularCollection, deserialized.regularCollection);
                assertEquals(original.obfuscatedMap, deserialized.obfuscatedMap);
                assertEquals(original.negateValueObfuscatedMap, deserialized.negateValueObfuscatedMap);
                assertEquals(original.regularMap, deserialized.regularMap);
                assertEquals(toLocalDates(original.obfuscatedDateList), toLocalDates(deserialized.obfuscatedDateList));

                assertEquals("<string>", deserialized.stringValue.toString());
                assertEquals("<default>", deserialized.dateValue.toString());
                assertEquals("[***]", deserialized.intArray.toString());
                assertEquals("<<13>>", deserialized.nestedClass.toString());
                assertEquals("********", deserialized.classWithSerializer.toString());
                assertEquals("[********, ********]", deserialized.obfuscatedList.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedList.toString());
                assertEquals("[foo, bar]", deserialized.regularList.toString());
                assertEquals("[********]", deserialized.obfuscatedSet.toString());
                assertEquals("[F***O]", deserialized.upperCaseObfuscatedSet.toString());
                assertEquals("[foo]", deserialized.regularSet.toString());
                assertEquals("[*****, *****]", deserialized.obfuscatedCollection.toString());
                assertEquals("[F***O, B***R]", deserialized.upperCaseObfuscatedCollection.toString());
                assertEquals("[foo, bar]", deserialized.regularCollection.toString());
                assertEquals("{1=******}", deserialized.obfuscatedMap.toString());
                assertEquals("{1=-***2}", deserialized.negateValueObfuscatedMap.toString());
                assertEquals("{1=2}", deserialized.regularMap.toString());
                assertEquals("[2020-05-**]", deserialized.obfuscatedDateList.toString());
            }
        }

        private List<LocalDate> toLocalDates(List<Date> dates) {
            return dates.stream()
                    .map(this::toLocalDate)
                    .collect(toList());
        }

        private LocalDate toLocalDate(Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        @Test
        @DisplayName("without access modifier fix")
        void testWithoutAccessModifierFix() {
            Module module = ObfuscationModule.defaultModule();

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(module)
                    .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS);

            int originalInstantiationCount = CustomCharacterRepresentationProvider.getInstantiationCount();

            TestClass original = new TestClass();

            StringWriter writer = new StringWriter();
            assertDoesNotThrow(() -> mapper.writeValue(writer, original));

            String json = writer.toString();

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> mapper.readValue(json, TestClass.class));
            assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
            assertEquals(getExpectedErrorMessage(UpperCase.class), exception.getCause().getMessage());

            assertEquals(originalInstantiationCount + 1, CustomCharacterRepresentationProvider.getInstantiationCount());
        }

        private String getExpectedErrorMessage(Class<?> cls) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ClassUtil.createInstance(cls, false));
            return exception.getMessage();
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class TestClass {

        @ObfuscateFixedValue("<null>")
        public Obfuscated<String> nullValue = null;

        @ObfuscateFixedValue("<string>")
        public Obfuscated<String> stringValue = Obfuscator.all().obfuscateObject("foo");

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        @JsonSerialize(using = DateSerializer.class)
        @JsonDeserialize(using = DateDeserializer.class)
        public Obfuscated<Date> dateValue = Obfuscator.all().obfuscateObject(calculateDate());

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(IntArrayToString.class)
        public Obfuscated<int[]> intArray = Obfuscator.all().obfuscateObject(new int[] { 1, 2, 3 });

        @ObfuscateNone
        @RepresentedBy(CustomCharacterRepresentationProvider.class)
        public Obfuscated<NestedClass> nestedClass = Obfuscator.all().obfuscateObject(new NestedClass());

        @ObfuscateFixedLength(8)
        @RepresentedBy(CustomCharacterRepresentationProvider.class)
        @JsonProperty("cws")
        public Obfuscated<ClassWithSerializer> classWithSerializer = Obfuscator.all().obfuscateObject(new ClassWithSerializer(1));

        public Obfuscated<Annotated> annotated = Obfuscator.all().obfuscateObject(new Annotated());

        @ObfuscateFixedLength(8)
        public List<String> obfuscatedList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(UpperCase.class)
        public List<String> upperCaseObfuscatedList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        public List<Annotated> annotatedList = Obfuscator.all().obfuscateList(Arrays.asList(new Annotated()));

        public List<String> regularList = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscateFixedLength(8)
        public Set<String> obfuscatedSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(UpperCase.class)
        public Set<String> upperCaseObfuscatedSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        public Set<Annotated> annotatedSet = Obfuscator.all().obfuscateSet(Collections.singleton(new Annotated()));

        public Set<String> regularSet = Obfuscator.all().obfuscateSet(Collections.singleton("foo"));

        @ObfuscateFixedLength(5)
        public Collection<String> obfuscatedCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(UpperCase.class)
        public Collection<String> upperCaseObfuscatedCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        public Collection<Annotated> annotatedCollection = Obfuscator.all().obfuscateCollection(Arrays.asList(new Annotated()));

        public Collection<String> regularCollection = Obfuscator.all().obfuscateList(Arrays.asList("foo", "bar"));

        @ObfuscateFixedLength(6)
        public Map<Integer, Integer> obfuscatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

        @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedTotalLength = 5)
        @RepresentedBy(NegateValueToString.class)
        public Map<Integer, Integer> negateValueObfuscatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

        public Map<String, Annotated> annotatedMap = Obfuscator.all().obfuscateMap(Collections.singletonMap("foo", new Annotated()));

        public Map<Integer, Integer> regularMap = Obfuscator.all().obfuscateMap(Collections.singletonMap(1, 2));

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

    public static final class CustomSerializer extends JsonSerializer<ClassWithSerializer> {

        @Override
        public void serialize(ClassWithSerializer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeNumber(value.intValue);
        }
    }

    public static final class CustomDeserializer extends JsonDeserializer<ClassWithSerializer> {

        @Override
        public ClassWithSerializer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            int intValue = p.getValueAsInt();
            return new ClassWithSerializer(intValue);
        }
    }
}
