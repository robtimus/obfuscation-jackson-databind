/*
 * DateFormat.java
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

import java.text.SimpleDateFormat;
import java.util.Date;
import com.github.robtimus.obfuscation.annotation.CharacterRepresentationProvider;

// This class needs to be public so it can be instantiated even with CAN_OVERRIDE_ACCESS_MODIFIERS disabled
@SuppressWarnings({ "javadoc", "nls" })
public final class DateFormat extends CharacterRepresentationProvider.TypeSpecific<Date> {

    public DateFormat() {
        super(Date.class);
    }

    @Override
    protected CharSequence convert(Date value) {
        return new SimpleDateFormat("yyyy-MM-dd").format(value);
    }
}
