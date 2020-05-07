/*
 * ObfuscatedSerializer.java
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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.robtimus.obfuscation.Obfuscated;

final class ObfuscatedSerializer extends JsonSerializer<Object> {

    private final BeanProperty property;
    private final JsonSerializer<Object> serializer;

    ObfuscatedSerializer(BeanProperty property, JsonSerializer<Object> serializer) {
        this.property = property;
        this.serializer = serializer;
    }

    @Override
    public void serialize(Object object, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Object value = ((Obfuscated<?>) object).value();
        JsonSerializer<Object> actualSerializer = serializer != null
                ? serializer
                : serializers.findTypedValueSerializer(value.getClass(), true, property);
        actualSerializer.serialize(value, gen, serializers);
    }
}
