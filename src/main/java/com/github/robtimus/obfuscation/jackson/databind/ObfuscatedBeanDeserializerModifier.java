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

    private final Map<Class<?>, Obfuscator> classObfuscators;
    private final Map<Class<?>, Obfuscator> interfaceObfuscators;

    private final Map<Class<?>, CharacterRepresentationProvider> classCharacterRepresentationProviders;
    private final Map<Class<?>, CharacterRepresentationProvider> interfaceCharacterRepresentationProviders;

    private final boolean requireObfuscatorAnnotation;

    ObfuscatedBeanDeserializerModifier(Obfuscator defaultObfuscator,
            Map<Class<?>, Obfuscator> classObfuscators,
            Map<Class<?>, Obfuscator> interfaceObfuscators,
            Map<Class<?>, CharacterRepresentationProvider> classCharacterRepresentationProviders,
            Map<Class<?>, CharacterRepresentationProvider> interfaceCharacterRepresentationProviders,
            boolean requireObfuscatorAnnotation) {

        this.defaultObfuscator = defaultObfuscator;

        this.classObfuscators = classObfuscators;
        this.interfaceObfuscators = interfaceObfuscators;
        this.classCharacterRepresentationProviders = classCharacterRepresentationProviders;
        this.interfaceCharacterRepresentationProviders = interfaceCharacterRepresentationProviders;

        this.requireObfuscatorAnnotation = requireObfuscatorAnnotation;
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

    // Obfuscated

    private JsonDeserializer<Object> createDeserializerForObfuscated(DeserializationConfig config, SettableBeanProperty property) {
        ObjectFactory objectFactory = FACTORY_MAPPER.apply(config);
        Optional<Obfuscator> optionalObfuscator = objectFactory.obfuscator(property::getAnnotation);
        if (!optionalObfuscator.isPresent()) {
            // property.getType() is Obfuscated<T>, so this returns the actual T
            Class<?> type = property.getType().getBindings().getBoundType(0).getRawClass();
            optionalObfuscator = findClassSpecificObfuscator(type, objectFactory);
        }
        Obfuscator obfuscator = optionalObfuscator.orElse(defaultObfuscator);
        return createDeserializerForObfuscated(property, obfuscator, objectFactory);
    }

    private JsonDeserializer<Object> createDeserializerForObfuscated(SettableBeanProperty property, Obfuscator obfuscator,
            ObjectFactory objectFactory) {

        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        // property.getType() is Obfuscated<T>, so index 0 is T
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(property, 0, objectFactory);
        return new ObfuscatedDeserializer.ForObfuscated(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    // List

    private Optional<JsonDeserializer<Object>> createDeserializerForList(DeserializationConfig config, SettableBeanProperty property) {
        ObjectFactory objectFactory = FACTORY_MAPPER.apply(config);
        Optional<Obfuscator> optionalObfuscator = objectFactory.obfuscator(property::getAnnotation);
        if (!optionalObfuscator.isPresent() && !requireObfuscatorAnnotation) {
            // property.getType() is List<T>, so this returns the actual T
            Class<?> type = property.getType().getBindings().getBoundType(0).getRawClass();
            optionalObfuscator = findClassSpecificObfuscator(type, objectFactory);
        }
        return optionalObfuscator.map(obfuscator -> createDeserializerForList(property, obfuscator, objectFactory));
    }

    private JsonDeserializer<Object> createDeserializerForList(SettableBeanProperty property, Obfuscator obfuscator, ObjectFactory objectFactory) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        // property.getType() is List<T>, so index 0 is T
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(property, 0, objectFactory);
        return new ObfuscatedDeserializer.ForList(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    // Set

    private Optional<JsonDeserializer<Object>> createDeserializerForSet(DeserializationConfig config, SettableBeanProperty property) {
        ObjectFactory objectFactory = FACTORY_MAPPER.apply(config);
        Optional<Obfuscator> optionalObfuscator = objectFactory.obfuscator(property::getAnnotation);
        if (!optionalObfuscator.isPresent() && !requireObfuscatorAnnotation) {
            // property.getType() is Set<T>, so this returns the actual T
            Class<?> type = property.getType().getBindings().getBoundType(0).getRawClass();
            optionalObfuscator = findClassSpecificObfuscator(type, objectFactory);
        }
        return optionalObfuscator.map(obfuscator -> createDeserializerForSet(property, obfuscator, objectFactory));
    }

    private JsonDeserializer<Object> createDeserializerForSet(SettableBeanProperty property, Obfuscator obfuscator, ObjectFactory objectFactory) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        // property.getType() is Set<T>, so index 0 is T
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(property, 0, objectFactory);
        return new ObfuscatedDeserializer.ForSet(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    // Collection

    private Optional<JsonDeserializer<Object>> createDeserializerForCollection(DeserializationConfig config, SettableBeanProperty property) {
        ObjectFactory objectFactory = FACTORY_MAPPER.apply(config);
        Optional<Obfuscator> optionalObfuscator = objectFactory.obfuscator(property::getAnnotation);
        if (!optionalObfuscator.isPresent() && !requireObfuscatorAnnotation) {
            // property.getType() is Collection<T>, so this returns the actual T
            Class<?> type = property.getType().getBindings().getBoundType(0).getRawClass();
            optionalObfuscator = findClassSpecificObfuscator(type, objectFactory);
        }
        return optionalObfuscator.map(obfuscator -> createDeserializerForCollection(property, obfuscator, objectFactory));
    }

    private JsonDeserializer<Object> createDeserializerForCollection(SettableBeanProperty property, Obfuscator obfuscator,
            ObjectFactory objectFactory) {

        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        // property.getType() is Collection<T>, so index 0 is T
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(property, 0, objectFactory);
        return new ObfuscatedDeserializer.ForCollection(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    // Map

    private Optional<JsonDeserializer<Object>> createDeserializerForMap(DeserializationConfig config, SettableBeanProperty property) {
        ObjectFactory objectFactory = FACTORY_MAPPER.apply(config);
        Optional<Obfuscator> optionalObfuscator = objectFactory.obfuscator(property::getAnnotation);
        if (!optionalObfuscator.isPresent() && !requireObfuscatorAnnotation) {
            // property.getType() is Map<K, V>, so this returns the actual V
            Class<?> type = property.getType().getBindings().getBoundType(1).getRawClass();
            optionalObfuscator = findClassSpecificObfuscator(type, objectFactory);
        }
        return optionalObfuscator.map(obfuscator -> createDeserializerForMap(property, obfuscator, objectFactory));
    }

    private JsonDeserializer<Object> createDeserializerForMap(SettableBeanProperty property, Obfuscator obfuscator, ObjectFactory objectFactory) {
        JsonDeserializer<Object> deserializer = property.getValueDeserializer();
        // property.getType() is Map<K, V>, so index 1 is V
        CharacterRepresentationProvider characterRepresentationProvider = getCharacterRepresentationProvider(property, 1, objectFactory);
        return new ObfuscatedDeserializer.ForMap(property, deserializer, obfuscator, characterRepresentationProvider);
    }

    // shared

    private Optional<Obfuscator> findClassSpecificObfuscator(Class<?> type, ObjectFactory objectFactory) {
        Obfuscator obfuscator = findClassSpecificObject(type, classObfuscators, interfaceObfuscators);
        return obfuscator != null ? Optional.of(obfuscator) : objectFactory.obfuscator(type::getAnnotation);
    }

    private CharacterRepresentationProvider getCharacterRepresentationProvider(BeanProperty property, int subTypeIndex, ObjectFactory objectFactory) {
        Optional<CharacterRepresentationProvider> optionalProvider = objectFactory.characterRepresentationProvider(property::getAnnotation);
        if (optionalProvider.isPresent()) {
            return optionalProvider.get();
        }

        Class<?> type = property.getType().getBindings().getBoundType(subTypeIndex).getRawClass();
        optionalProvider = findClassSpecificCharacterRepresentationProvider(type, objectFactory);
        return optionalProvider.orElseGet(() -> CharacterRepresentationProvider.getDefaultInstance(type));
    }

    private Optional<CharacterRepresentationProvider> findClassSpecificCharacterRepresentationProvider(Class<?> type, ObjectFactory objectFactory) {
        CharacterRepresentationProvider provider = findClassSpecificObject(type, classCharacterRepresentationProviders,
                interfaceCharacterRepresentationProviders);

        return provider != null ? Optional.of(provider) : objectFactory.characterRepresentationProvider(type::getAnnotation);
    }

    private void replaceProperty(SettableBeanProperty property, JsonDeserializer<Object> newDeserializer,
            Map<String, SettableBeanProperty> propertyReplacements) {

        SettableBeanProperty replacement = property.withValueDeserializer(newDeserializer);
        propertyReplacements.put(property.getName(), replacement);
    }

    // class-based lookups

    static <T> T findClassSpecificObject(Class<?> type, Map<Class<?>, T> classMappings, Map<Class<?>, T> interfaceMappings) {
        // Try direct match first
        T result = findDirectClassSpecificObject(type, classMappings, interfaceMappings);
        if (result != null) {
            return result;
        }

        if (!interfaceMappings.isEmpty()) {
            // Try super-interfaces
            result = findInterfaceSpecificObject(type, interfaceMappings);
            if (result != null) {
                return result;
            }

            // Try interfaces of super classes
            if (!type.isInterface()) {
                Class<?> iterator = type.getSuperclass();
                while (iterator != null) {
                    result = findInterfaceSpecificObject(iterator, interfaceMappings);
                    if (result != null) {
                        return result;
                    }
                    iterator = iterator.getSuperclass();
                }
            }
        }
        return null;
    }

    private static <T> T findDirectClassSpecificObject(Class<?> type, Map<Class<?>, T> classMappings, Map<Class<?>, T> interfaceMappings) {
        if (type.isInterface()) {
            // no need to check for interfaceMappings.isEmpty(), as interfaceMappings.get(type) will then return null anyway
            return interfaceMappings.get(type);
        }
        if (!classMappings.isEmpty()) {
            Class<?> iterator = type;
            while (iterator != null) {
                T result = classMappings.get(iterator);
                if (result != null) {
                    return result;
                }
                iterator = iterator.getSuperclass();
            }
        }
        return null;
    }

    private static <T> T findInterfaceSpecificObject(Class<?> type, Map<Class<?>, T> interfaceMappings) {
        for (Class<?> iface : type.getInterfaces()) {
            T result = interfaceMappings.get(iface);
            if (result != null) {
                return result;
            }
            result = findInterfaceSpecificObject(iface, interfaceMappings);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
