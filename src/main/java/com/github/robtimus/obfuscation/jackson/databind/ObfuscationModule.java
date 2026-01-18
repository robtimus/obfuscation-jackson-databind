/*
 * ObfuscationModule.java
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.BooleanArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.ByteArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.CharArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.DoubleArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.FloatArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.IntArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.LongArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.ObjectArrayToString;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider.ShortArrayToString;
import com.github.robtimus.obfuscation.annotation.ObjectFactory;
import tools.jackson.core.Version;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.util.ClassUtil;

/**
 * A module that adds support for serializing and deserializing obfuscated values.
 *
 * @author Rob Spoor
 */
public final class ObfuscationModule extends JacksonModule {

    private static final ObfuscationModule DEFAULT_MODULE = builder().build();

    private final ObjectFactory objectFactory;
    private final Obfuscator defaultObfuscator;

    private final Map<Class<?>, Obfuscator> classObfuscators;
    private final Map<Class<?>, Obfuscator> interfaceObfuscators;

    private final Map<Class<?>, CharacterRepresentationProvider> classCharacterRepresentationProviders;
    private final Map<Class<?>, CharacterRepresentationProvider> interfaceCharacterRepresentationProviders;

    private final boolean requireObfuscatorAnnotation;

    private ObfuscationModule(Builder builder) {
        objectFactory = builder.objectFactory;
        defaultObfuscator = builder.defaultObfuscator;

        classObfuscators = copyMap(builder.classObfuscators);
        interfaceObfuscators = copyMap(builder.interfaceObfuscators);
        classCharacterRepresentationProviders = copyMap(builder.classCharacterRepresentationProviders);
        interfaceCharacterRepresentationProviders = copyMap(builder.interfaceCharacterRepresentationProviders);

        requireObfuscatorAnnotation = builder.requireObfuscatorAnnotation;
    }

    private static <T> Map<Class<?>, T> copyMap(Map<Class<?>, T> map) {
        return map != null
                ? Collections.unmodifiableMap(new HashMap<>(map))
                : Collections.emptyMap();
    }

    @Override
    public String getModuleName() {
        return getClass().getName();
    }

    @Override
    public Version version() {
        return ModuleVersion.VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializerModifier(new ObfuscatedBeanSerializerModifier());
        context.addDeserializerModifier(new ObfuscatedBeanDeserializerModifier(objectFactory, defaultObfuscator,
                classObfuscators, interfaceObfuscators,
                classCharacterRepresentationProviders, interfaceCharacterRepresentationProviders,
                requireObfuscatorAnnotation));
    }

    /**
     * Returns a module with all settings set to default.
     *
     * @return A module with all settings set to default.
     */
    public static ObfuscationModule defaultModule() {
        return DEFAULT_MODULE;
    }

    /**
     * Returns a builder that will create {@code ObfuscatedModules}.
     *
     * @return A builder that will create {@code ObfuscatedModules}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link ObfuscationModule ObfuscationModules}.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private static final Obfuscator DEFAULT_OBFUSCATOR = Obfuscator.fixedLength(3);

        private ObjectFactory objectFactory;
        private Obfuscator defaultObfuscator = DEFAULT_OBFUSCATOR;

        private Map<Class<?>, Obfuscator> classObfuscators;
        private Map<Class<?>, Obfuscator> interfaceObfuscators;

        private Map<Class<?>, CharacterRepresentationProvider> classCharacterRepresentationProviders;
        private Map<Class<?>, CharacterRepresentationProvider> interfaceCharacterRepresentationProviders;

        private boolean requireObfuscatorAnnotation = false;

        private Builder() {
            super();
        }

        /**
         * Sets the object factory to use. This method can be used to override the default object factory, for instance to use an object factory that
         * uses a Spring bean factory or CDI lookup. The default uses {@link ClassUtil#createInstance(Class, boolean)}, with
         * {@link DeserializationConfig#canOverrideAccessModifiers()} providing the boolean flag.
         *
         * @param objectFactory The object factory to use, or {@code null} to use a default object factory.
         * @return This object.
         * @see MapperFeature#CAN_OVERRIDE_ACCESS_MODIFIERS
         * @since 1.2
         */
        public Builder withObjectFactory(ObjectFactory objectFactory) {
            this.objectFactory = objectFactory;
            return this;
        }

        /**
         * Sets the default obfuscator to use, in case no obfuscator could be found from annotations.
         * The default is {@link Obfuscator#fixedLength(int) Obfuscator.fixedLength(3)}.
         *
         * @param defaultObfuscator The default obfuscator to use.
         * @return This object.
         * @throws NullPointerException If the given obfuscator is {@code null}.
         */
        public Builder withDefaultObfuscator(Obfuscator defaultObfuscator) {
            this.defaultObfuscator = Objects.requireNonNull(defaultObfuscator);
            return this;
        }

        /**
         * Sets the default obfuscator to use for a specific type, in case no obfuscator could be found from annotations.
         * This type should be the generic type of any {@link Obfuscated}, {@link List}, {@link Set}, {@link Collection} or {@link Map}.
         * It doesn't need to be the exact type; it can also be a super class or implemented interface.
         *
         * @param type The type to set the default obfuscator for.
         * @param defaultObfuscator The default obfuscator to use.
         * @return This object.
         * @throws NullPointerException If the given type or obfuscator is {@code null}.
         * @since 1.1
         */
        public Builder withDefaultObfuscator(Class<?> type, Obfuscator defaultObfuscator) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(defaultObfuscator);

            if (type.isInterface()) {
                if (interfaceObfuscators == null) {
                    interfaceObfuscators = new HashMap<>();
                }
                interfaceObfuscators.put(type, defaultObfuscator);
            } else {
                if (classObfuscators == null) {
                    classObfuscators = new HashMap<>();
                }
                classObfuscators.put(type, defaultObfuscator);
            }
            return this;
        }

        /**
         * Sets whether or not to require an annotation to provide an obfuscator for {@link List}, {@link Set}, {@link Collection} and {@link Map}
         * fields. If not, then any {@link List}, {@link Set}, {@link Collection} and {@link Map} field with a generic type for which a
         * {@link #withDefaultObfuscator(Class, Obfuscator) default obfuscator} was specified will be obfuscated during deserialization.
         * The default is {@code false}.
         *
         * @param requireObfuscatorAnnotation {@code true} to require an annotation to provide an obfuscator,
         *                                        or {@code false} to use default obfuscators if present.
         * @return This object.
         * @since 1.1
         */
        public Builder requireObfuscatorAnnotation(boolean requireObfuscatorAnnotation) {
            this.requireObfuscatorAnnotation = requireObfuscatorAnnotation;
            return this;
        }

        /**
         * Sets the default character representation provider to use for a specific type, in case no character representation provider could be found
         * from annotations.
         * This type should be the generic type of any {@link Obfuscated}, {@link List}, {@link Set}, {@link Collection} or {@link Map}.
         * It doesn't need to be the exact type; it can also be a super class or implemented interface.
         * <p>
         * By default, the following character representation providers are already registered:
         * <ul>
         * <li>{@link BooleanArrayToString} for {@code boolean[]}</li>
         * <li>{@link CharArrayToString} for {@code char[]}</li>
         * <li>{@link ByteArrayToString} for {@code byte[]}</li>
         * <li>{@link ShortArrayToString} for {@code short[]}</li>
         * <li>{@link IntArrayToString} for {@code int[]}</li>
         * <li>{@link LongArrayToString} for {@code long[]}</li>
         * <li>{@link FloatArrayToString} for {@code float[]}</li>
         * <li>{@link DoubleArrayToString} for {@code double[]}</li>
         * <li>{@link ObjectArrayToString} for {@code Object[]}</li>
         * </ul>
         *
         * @param type The type to set the default obfuscator for.
         * @param defaultProvider The default character representation provider to use.
         * @return This object.
         * @throws NullPointerException If the given type or character representation provider is {@code null}.
         * @since 1.1
         */
        public Builder withDefaultCharacterRepresentation(Class<?> type, CharacterRepresentationProvider defaultProvider) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(defaultProvider);

            if (type.isInterface()) {
                if (interfaceCharacterRepresentationProviders == null) {
                    interfaceCharacterRepresentationProviders = new HashMap<>();
                }
                interfaceCharacterRepresentationProviders.put(type, defaultProvider);
            } else {
                if (classCharacterRepresentationProviders == null) {
                    classCharacterRepresentationProviders = new HashMap<>();
                }
                classCharacterRepresentationProviders.put(type, defaultProvider);
            }
            return this;
        }

        /**
         * Creates a new {@link ObfuscationModule} with the current settings of this builder.
         *
         * @return The created {@link ObfuscationModule}.
         */
        public ObfuscationModule build() {
            return new ObfuscationModule(this);
        }
    }
}
