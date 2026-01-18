/*
 * ObfuscatedBeanSerializerModifier.java
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

import java.util.List;
import java.util.ListIterator;
import com.github.robtimus.obfuscation.Obfuscated;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

final class ObfuscatedBeanSerializerModifier extends ValueSerializerModifier {

    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> properties = super.changeProperties(config, beanDesc, beanProperties);
        for (ListIterator<BeanPropertyWriter> i = properties.listIterator(); i.hasNext(); ) {
            BeanPropertyWriter property = i.next();
            if (property.getType().getRawClass() == Obfuscated.class) {
                i.set(new ObfuscatedBeanPropertyWriter(property));
            }
        }
        return properties;
    }
}
