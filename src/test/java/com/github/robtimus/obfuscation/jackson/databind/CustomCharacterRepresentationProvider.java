/*
 * CustomCharacterRepresentationProvider.java
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

import java.util.concurrent.atomic.AtomicInteger;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;
import com.github.robtimus.obfuscation.jackson.databind.ObfuscationModuleTest.NestedClass;

// This class needs to be public so it can be instantiated even with CAN_OVERRIDE_ACCESS_MODIFIERS disabled
@SuppressWarnings({ "javadoc", "nls" })
public final class CustomCharacterRepresentationProvider extends CharacterRepresentationProvider.TypeSpecific<NestedClass> {

    private static final AtomicInteger INSTANTIATION_COUNT = new AtomicInteger();

    public CustomCharacterRepresentationProvider() {
        super(NestedClass.class);
        INSTANTIATION_COUNT.incrementAndGet();
    }

    @Override
    protected CharSequence convert(NestedClass value) {
        return "<<" + value.intValue + ">>";
    }

    public static int getInstantiationCount() {
        return INSTANTIATION_COUNT.get();
    }
}
