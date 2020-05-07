/*
 * ObfuscatedBeanDeserializerModifier.java
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;

final class ObfuscatedBeanDeserializerModifier extends BeanDeserializerModifier {

    private final Obfuscator defaultObfuscator;

    ObfuscatedBeanDeserializerModifier(Obfuscator defaultObfuscator) {
        this.defaultObfuscator = defaultObfuscator;
    }

    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        BeanDeserializerBuilder updatedBuilder = super.updateBuilder(config, beanDesc, builder);
        Map<String, SettableBeanProperty> propertyReplacements = new LinkedHashMap<>();
        for (Iterator<SettableBeanProperty> i = updatedBuilder.getProperties(); i.hasNext(); ) {
            SettableBeanProperty property = i.next();
            if (property.getType().getRawClass() == Obfuscated.class) {
                property = property.withValueDeserializer(new ObfuscatedDeserializer(property, property.getValueDeserializer(), defaultObfuscator));
                propertyReplacements.put(property.getName(), property);
            }
        }
        for (SettableBeanProperty property : propertyReplacements.values()) {
            updatedBuilder.addOrReplaceProperty(property, true);
        }
        return updatedBuilder;
    }
}
