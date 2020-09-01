/*
 * ObfuscatedDeserializer.java
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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.robtimus.obfuscation.Obfuscator;

abstract class ObfuscatedDeserializer extends JsonDeserializer<Object> {

    final BeanProperty property;
    private final JsonDeserializer<Object> deserializer;
    private final JavaType valueType;
    final Obfuscator obfuscator;
    final Function<Object, ? extends CharSequence> stringRepresentation;

    ObfuscatedDeserializer(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator obfuscator,
            Function<Object, ? extends CharSequence> stringRepresentation) {

        this.property = property;
        this.deserializer = serializer;
        this.obfuscator = obfuscator;
        this.stringRepresentation = stringRepresentation;

        valueType = extractJavaType();
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> actualDeserializer = deserializer != null
                ? deserializer
                : ctxt.findContextualValueDeserializer(valueType, property);

        Object value = actualDeserializer.deserialize(p, ctxt);
        return obfuscateValue(value);
    }

    abstract JavaType extractJavaType();

    abstract Object obfuscateValue(Object value);

    static final class ForObfuscated extends ObfuscatedDeserializer {

        ForObfuscated(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator obfuscator,
                Function<Object, ? extends CharSequence> stringRepresentation) {

            super(property, serializer, obfuscator, stringRepresentation);
        }

        @Override
        JavaType extractJavaType() {
            // property.getType() is Obfuscated<T>, so this returns the actual T
            return property.getType().getBindings().getTypeParameters().get(0);
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateObject(value, () -> stringRepresentation.apply(value));
        }
    }

    static final class ForList extends ObfuscatedDeserializer {

        ForList(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator obfuscator,
                Function<Object, ? extends CharSequence> stringRepresentation) {

            super(property, serializer, obfuscator, stringRepresentation);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateList((List<?>) value, stringRepresentation);
        }
    }

    static final class ForSet extends ObfuscatedDeserializer {

        ForSet(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator obfuscator,
                Function<Object, ? extends CharSequence> stringRepresentation) {

            super(property, serializer, obfuscator, stringRepresentation);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateSet((Set<?>) value, stringRepresentation);
        }
    }

    static final class ForCollection extends ObfuscatedDeserializer {

        ForCollection(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator obfuscator,
                Function<Object, ? extends CharSequence> stringRepresentation) {

            super(property, serializer, obfuscator, stringRepresentation);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateCollection((Collection<?>) value, stringRepresentation);
        }
    }

    static final class ForMap extends ObfuscatedDeserializer {

        ForMap(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator obfuscator,
                Function<Object, ? extends CharSequence> stringRepresentation) {

            super(property, serializer, obfuscator, stringRepresentation);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateMap((Map<?, ?>) value, stringRepresentation);
        }
    }
}
