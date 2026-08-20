package org.jmod.dsl.module.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;

/**
 * Reads public configuration fields, subclass hiding parent fields of the same name.
 */
public final class ConfigFields {
    private ConfigFields() {
    }

    public static Map<String, String> read(ExternalConfiguration instance) {
        Map<String, String> result = new LinkedHashMap<>();
        if (instance == null) {
            return result;
        }
        Class<?> cls = instance.getClass();
        while (cls != null && ExternalConfiguration.class.isAssignableFrom(cls)
                && cls != ExternalConfiguration.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (result.containsKey(field.getName()) || !isConfigField(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(instance);
                    result.put(field.getName(), value == null ? "" : String.valueOf(value));
                } catch (IllegalAccessException ignored) {
                    // skip inaccessible fields
                }
            }
            cls = cls.getSuperclass();
        }
        return result;
    }

    public static boolean isConfigField(Field field) {
        if (field == null || field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        return isConfigFieldType(field.getType());
    }

    public static boolean isConfigFieldType(Class<?> type) {
        return type == boolean.class || type == int.class || type == long.class
                || type == float.class || type == double.class
                || type == Boolean.class || type == Integer.class || type == Long.class
                || type == Float.class || type == Double.class || type == String.class;
    }
}
