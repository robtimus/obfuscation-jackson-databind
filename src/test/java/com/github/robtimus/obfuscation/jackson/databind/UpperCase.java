/*
 * UpperCase.java
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

import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;

// This class is not public, so it cannot be instantiated if CAN_OVERRIDE_ACCESS_MODIFIERS is disabled
final class UpperCase implements CharacterRepresentationProvider {

    @Override
    public CharSequence toCharSequence(Object value) {
        return value.toString().toUpperCase();
    }
}
