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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

abstract class ObfuscatedDeserializer extends ValueDeserializer<Object> {

    final BeanProperty property;
    private final ValueDeserializer<Object> deserializer;
    private final JavaType valueType;
    final Obfuscator obfuscator;
    final CharacterRepresentationProvider characterRepresentationProvider;

    ObfuscatedDeserializer(BeanProperty property, ValueDeserializer<Object> serializer, Obfuscator obfuscator,
            CharacterRepresentationProvider characterRepresentationProvider) {

        this.property = property;
        this.deserializer = serializer;
        this.obfuscator = obfuscator;
        this.characterRepresentationProvider = characterRepresentationProvider;

        valueType = extractJavaType();
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        ValueDeserializer<Object> actualDeserializer = deserializer != null
                ? deserializer
                : ctxt.findContextualValueDeserializer(valueType, property);

        Object value = actualDeserializer.deserialize(p, ctxt);
        return obfuscateValue(value);
    }

    abstract JavaType extractJavaType();

    abstract Object obfuscateValue(Object value);

    static final class ForObfuscated extends ObfuscatedDeserializer {

        ForObfuscated(BeanProperty property, ValueDeserializer<Object> serializer, Obfuscator obfuscator,
                CharacterRepresentationProvider characterRepresentationProvider) {

            super(property, serializer, obfuscator, characterRepresentationProvider);
        }

        @Override
        JavaType extractJavaType() {
            // property.getType() is Obfuscated<T>, so this returns the actual T
            return property.getType().getBindings().getBoundType(0);
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateObject(value, () -> characterRepresentationProvider.toCharSequence(value));
        }
    }

    static final class ForList extends ObfuscatedDeserializer {

        ForList(BeanProperty property, ValueDeserializer<Object> serializer, Obfuscator obfuscator,
                CharacterRepresentationProvider characterRepresentationProvider) {

            super(property, serializer, obfuscator, characterRepresentationProvider);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateList((List<?>) value, characterRepresentationProvider::toCharSequence);
        }
    }

    static final class ForSet extends ObfuscatedDeserializer {

        ForSet(BeanProperty property, ValueDeserializer<Object> serializer, Obfuscator obfuscator,
                CharacterRepresentationProvider characterRepresentationProvider) {

            super(property, serializer, obfuscator, characterRepresentationProvider);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateSet((Set<?>) value, characterRepresentationProvider::toCharSequence);
        }
    }

    static final class ForCollection extends ObfuscatedDeserializer {

        ForCollection(BeanProperty property, ValueDeserializer<Object> serializer, Obfuscator obfuscator,
                CharacterRepresentationProvider characterRepresentationProvider) {

            super(property, serializer, obfuscator, characterRepresentationProvider);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateCollection((Collection<?>) value, characterRepresentationProvider::toCharSequence);
        }
    }

    static final class ForMap extends ObfuscatedDeserializer {

        ForMap(BeanProperty property, ValueDeserializer<Object> serializer, Obfuscator obfuscator,
                CharacterRepresentationProvider characterRepresentationProvider) {

            super(property, serializer, obfuscator, characterRepresentationProvider);
        }

        @Override
        JavaType extractJavaType() {
            return property.getType();
        }

        @Override
        Object obfuscateValue(Object value) {
            return obfuscator.obfuscateMap((Map<?, ?>) value, characterRepresentationProvider::toCharSequence);
        }
    }
}
