/*
 * ObfuscatedModule.java
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

import java.util.Objects;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;

/**
 * A module that adds support for {@link Obfuscated}.
 *
 * @author Rob Spoor
 */
public final class ObfuscatedModule extends Module {

    private static final ObfuscatedModule DEFAULT_MODULE = builder().build();

    private final Obfuscator defaultObfuscator;

    private ObfuscatedModule(Builder builder) {
        defaultObfuscator = builder.defaultObfuscator;
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
        context.addBeanSerializerModifier(new ObfuscatedBeanSerializerModifier());
        context.addBeanDeserializerModifier(new ObfuscatedBeanDeserializerModifier(defaultObfuscator));
    }

    /**
     * Returns a module with all settings set to default.
     *
     * @return A module with all settings set to default.
     */
    public static ObfuscatedModule defaultModule() {
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
     * A builder for {@link ObfuscatedModule ObfuscatedModules}.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private static final Obfuscator DEFAULT_OBFUSCATOR = Obfuscator.fixedLength(3);

        private Obfuscator defaultObfuscator = DEFAULT_OBFUSCATOR;

        private Builder() {
            super();
        }

        /**
         * Sets the default obfuscator to use, in case no obfuscator could be found from annotations.
         * The default is {@link Obfuscator#fixedLength(int) Obfuscator.fixedLength(3)}.
         *
         * @param defaultObfuscator The default obfuscator to use.
         * @return This object.
         */
        public Builder withDefaultObfuscator(Obfuscator defaultObfuscator) {
            this.defaultObfuscator = Objects.requireNonNull(defaultObfuscator);
            return this;
        }

        /**
         * Creates a new {@link ObfuscatedModule} with the current settings of this builder.
         *
         * @return The created {@link ObfuscatedModule}.
         */
        public ObfuscatedModule build() {
            return new ObfuscatedModule(this);
        }
    }
}
