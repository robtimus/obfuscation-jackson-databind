/*
 * ObfuscatedTest.java
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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
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
import com.github.robtimus.obfuscation.annotation.StringRepresentationProvider;
import com.github.robtimus.obfuscation.annotation.StringRepresentationProvider.IntArrayToString;

@SuppressWarnings({ "javadoc", "nls" })
public class ObfuscatedTest {

    @Test
    @DisplayName("serialize")
    public void testSerialize() throws IOException {
        Module module = ObfuscatedModule.defaultModule();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(module);

        TestClass original = new TestClass();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, original);

        String json = writer.toString();
        assertThat(json, containsString("\"dateValue\":\"2020-05-07T12:30:55.123+0000\""));
        assertThat(json, containsString("\"intArray\":[1,2,3]"));
        assertThat(json, containsString("\"stringValue\":\"foo\""));
        assertThat(json, containsString("\"nestedClass\":{\"intValue\":13}"));
        assertThat(json, containsString("\"cws\":1"));

        TestClass deserialized = mapper.readValue(json, TestClass.class);

        assertEquals(original.stringValue, deserialized.stringValue);
        assertEquals(original.dateValue, deserialized.dateValue);
        assertNotNull(deserialized.intArray);
        assertArrayEquals(original.intArray.value(), deserialized.intArray.value());
        assertNotNull(deserialized.nestedClass);
        assertEquals(original.nestedClass.value().intValue, deserialized.nestedClass.value().intValue);
        assertNotNull(deserialized.classWithSerializer);
        assertEquals(original.classWithSerializer.value().intValue, deserialized.classWithSerializer.value().intValue);

        assertEquals("<string>", deserialized.stringValue.toString());
        assertEquals("***", deserialized.dateValue.toString());
        assertEquals("[***]", deserialized.intArray.toString());
        assertEquals("<<13>>", deserialized.nestedClass.toString());
        assertEquals("********", deserialized.classWithSerializer.toString());
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class TestClass {

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
    private static final class NestedClass {

        private int intValue = 13;
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

    public static final class CustomStringRepresentationProvider extends StringRepresentationProvider.ForValueType<NestedClass> {

        public CustomStringRepresentationProvider() {
            super(NestedClass.class);
        }

        @Override
        protected Supplier<? extends CharSequence> typeSpecificStringRepresentation(NestedClass value) {
            return () -> "<<" + value.intValue + ">>";
        }
    }
}
