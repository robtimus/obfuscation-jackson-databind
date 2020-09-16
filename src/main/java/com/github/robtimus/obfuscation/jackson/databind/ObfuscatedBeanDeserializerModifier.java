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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;
import com.github.robtimus.obfuscation.annotation.ObjectFactory;

final class ObfuscatedBeanDeserializerModifier extends BeanDeserializerModifier {

    private static final ObjectFactory CAN_OVERRIDE_ACCESS_MODIFIERS = ObfuscatedBeanDeserializerModifier::createInstanceWithCanFixAccess;
    private static final ObjectFactory CANNOT_OVERRIDE_ACCESS_MODIFIERS = ObfuscatedBeanDeserializerModifier::createInstanceWithoutCanFixAccess;
    private static final Function<DeserializationConfig, ObjectFactory> FACTORY_MAPPER = config -> config.canOverrideAccessModifiers()
            ? CAN_OVERRIDE_ACCESS_MODIFIERS
            : CANNOT_OVERRIDE_ACCESS_MODIFIERS;

    private final Obfuscator defaultObfuscator;

    ObfuscatedBeanDeserializerModifier(Obfuscator defaultObfuscator) {
        this.defaultObfuscator = defaultObfuscator;
    }

    private static <T> T createInstanceWithCanFixAccess(Class<T> type) {
        return ClassUtil.createInstance(type, true);
    }

    private static <T> T createInstanceWithoutCanFixAccess(Class<T> type) {
        return ClassUtil.createInstance(type, false);
    }

    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
        BeanDeserializerBuilder updatedBuilder = super.updateBuilder(config, beanDesc, builder);
        Map<String, SettableBeanProperty> propertyReplacements = new LinkedHashMap<>();
        for (Iterator<SettableBeanProperty> i = updatedBuilder.getProperties(); i.hasNext(); ) {
            SettableBeanProperty property = i.next();
            Class<?> rawPropertyType = property.getType().getRawClass();

            // These if-statements check for exact interface declarations, so the obfuscating replacement will have a compatible type

            if (rawPropertyType == Obfuscated.class) {
                JsonDeserializer<Object> newDeserializer = createDeserializerForObfuscated(config, property);
                replaceProperty(property, newDeserializer, propertyReplacements);

            } else if (rawPropertyType == List.class) {
                createDeserializerForList(config, property)
                        .ifPresent(newDeserializer -> replaceProperty(property, newDeserializer, propertyReplacements));

            } else if (rawPropertyType == Set.class) {
                createDeserializerForSet(config, property)
                        .ifPresent(newDeserializer -> replaceProperty(property, newDeserializer, propertyReplacements));

            } else if (rawPropertyType == Collection.class) {
                createDeserializerForCollection(config, property)
                        .ifPresent(newDeserializer -> replaceProperty(property, newDeserializer, propertyReplacements));

            } else if (rawPropertyType == Map.class) {
                createDeserializerForMap(config, property)
                        .ifPresent(newDeserializer -> replaceProperty(property, newDeserializer, propertyReplacements));
            }
        }
        for (SettableBeanProperty property : propertyReplacements.values()) {
            updatedBuilder.addOrReplaceProperty(property, true);
        }
        return updatedBuilder;
    }

    private JsonDeserializer<Object> createDeserializerForObfuscated(DeserializationConfig config, SettableBeanProperty property) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        Obfuscator obfuscator = FACTORY_MAPPER.apply(config).obfuscator(property::getAnnotation)
                .orElse(defaultObfuscator);
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(config, property);
        return new ObfuscatedDeserializer.ForObfuscated(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    private Optional<JsonDeserializer<Object>> createDeserializerForList(DeserializationConfig config, SettableBeanProperty property) {
        Optional<Obfuscator> obfuscator = FACTORY_MAPPER.apply(config).obfuscator(property::getAnnotation);
        return obfuscator.map(o -> createDeserializerForList(config, property, o));
    }

    private JsonDeserializer<Object> createDeserializerForList(DeserializationConfig config, SettableBeanProperty property, Obfuscator obfuscator) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(config, property);
        return new ObfuscatedDeserializer.ForList(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    private Optional<JsonDeserializer<Object>> createDeserializerForSet(DeserializationConfig config, SettableBeanProperty property) {
        Optional<Obfuscator> obfuscator = FACTORY_MAPPER.apply(config).obfuscator(property::getAnnotation);
        return obfuscator.map(o -> createDeserializerForSet(config, property, o));
    }

    private JsonDeserializer<Object> createDeserializerForSet(DeserializationConfig config, SettableBeanProperty property, Obfuscator obfuscator) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(config, property);
        return new ObfuscatedDeserializer.ForSet(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    private Optional<JsonDeserializer<Object>> createDeserializerForCollection(DeserializationConfig config, SettableBeanProperty property) {
        Optional<Obfuscator> obfuscator = FACTORY_MAPPER.apply(config).obfuscator(property::getAnnotation);
        return obfuscator.map(o -> createDeserializerForCollection(config, property, o));
    }

    private JsonDeserializer<Object> createDeserializerForCollection(DeserializationConfig config, SettableBeanProperty property,
            Obfuscator obfuscator) {

        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(config, property);
        return new ObfuscatedDeserializer.ForCollection(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    private Optional<JsonDeserializer<Object>> createDeserializerForMap(DeserializationConfig config, SettableBeanProperty property) {
        Optional<Obfuscator> obfuscator = FACTORY_MAPPER.apply(config).obfuscator(property::getAnnotation);
        return obfuscator.map(o -> createDeserializerForMap(config, property, o));
    }

    private JsonDeserializer<Object> createDeserializerForMap(DeserializationConfig config, SettableBeanProperty property, Obfuscator obfuscator) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(config, property);
        return new ObfuscatedDeserializer.ForMap(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    private void replaceProperty(SettableBeanProperty property, JsonDeserializer<Object> newDeserializer,
            Map<String, SettableBeanProperty> propertyReplacements) {

        SettableBeanProperty replacement = property.withValueDeserializer(newDeserializer);
        propertyReplacements.put(property.getName(), replacement);
    }

    private CharacterRepresentationProvider getCharacterRepresentationProvider(DeserializationConfig config, BeanProperty property) {
        return FACTORY_MAPPER.apply(config).characterRepresentationProvider(property::getAnnotation)
                .orElse(CharacterRepresentationProvider.ToString.INSTANCE);
    }
}
