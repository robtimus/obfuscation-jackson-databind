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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.ObfuscatorFactory;
import com.github.robtimus.obfuscation.annotation.RepresentedBy;
import com.github.robtimus.obfuscation.annotation.StringRepresentationProvider;

final class ObfuscatedDeserializer extends JsonDeserializer<Object> {

    private final BeanProperty property;
    private final JsonDeserializer<Object> deserializer;
    private final JavaType valueType;
    private final Obfuscator obfuscator;
    private final StringRepresentationProvider representationProvider;

    ObfuscatedDeserializer(BeanProperty property, JsonDeserializer<Object> serializer, Obfuscator defaultObfuscator) {
        this.property = property;
        this.deserializer = serializer;

        // property.getType() is Obfuscated<T>, so returns gets T
        valueType = property.getType().getBindings().getTypeParameters().get(0);
        obfuscator = ObfuscatorFactory.createObfuscator(property::getAnnotation)
                .orElse(defaultObfuscator);
        representationProvider = getRepresentationProvider();
    }

    private StringRepresentationProvider getRepresentationProvider() {
        RepresentedBy representedBy = property.getAnnotation(RepresentedBy.class);
        return representedBy != null
                ? StringRepresentationProvider.createInstance(representedBy.value())
                : StringRepresentationProvider.ToString.INSTANCE;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> actualDeserializer = deserializer != null
                ? deserializer
                : ctxt.findContextualValueDeserializer(valueType, property);

        Object value = actualDeserializer.deserialize(p, ctxt);
        return value != null
                ? obfuscator.obfuscateObject(value, representationProvider.stringRepresentation(value))
                : null;
    }
}
