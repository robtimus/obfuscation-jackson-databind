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

import com.github.robtimus.obfuscation.Obfuscated;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

final class ObfuscatedSerializer extends ValueSerializer<Object> {

    private final BeanProperty property;
    private final ValueSerializer<Object> serializer;

    ObfuscatedSerializer(BeanProperty property, ValueSerializer<Object> serializer) {
        this.property = property;
        this.serializer = serializer;
    }

    @Override
    public void serialize(Object object, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        Object value = ((Obfuscated<?>) object).value();
        ValueSerializer<Object> actualSerializer = serializer != null
                ? serializer
                : ctxt.findContentValueSerializer(value.getClass(), property);
        actualSerializer.serialize(value, gen, ctxt);
    }
}
