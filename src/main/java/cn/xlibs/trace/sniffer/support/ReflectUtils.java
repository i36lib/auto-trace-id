package cn.xlibs.trace.sniffer.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The reflection utils
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class ReflectUtils {
    private ReflectUtils(){}

    /**
     * The instance or class field cache
     */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    /**
     * The instance or class method cache
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * Get a wrapped method
     * @param className the class which the method in
     * @param methodName method name
     * @param parameterTypes method parameter types
     * @return the wrapped method
     */
    public static MethodWrapper getMethod(String className, String methodName, Class<?>... parameterTypes) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return new MethodWrapper(clazz, methodName, parameterTypes);
    }

    /**
     * Get a wrapped method
     * @param obj instance or class
     * @param methodName method name
     * @param parameterTypes method parameter types
     * @return the wrapped method
     */
    public static MethodWrapper getMethod(Object obj, String methodName, Class<?>... parameterTypes) {
        return new MethodWrapper(obj, methodName, parameterTypes);
    }

    /**
     * Get field of an instance or class
     * @param obj instance or class
     * @param fieldName field name
     * @return the Field
     */
    public static Field getField(Object obj, String fieldName) {
        if (Objects.isNull(obj)) {
            return null;
        }

        Class<?> clazz = obj instanceof Class<?> ? (Class<?>)obj : obj.getClass();
        String cacheKey = clazz.getCanonicalName() + "." + fieldName;
        Field field = FIELD_CACHE.get(cacheKey);
        if (Objects.isNull(field)) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(cacheKey, field);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return field;
    }

    /**
     * Get value from an instance or class field
     * @param obj instance or class
     * @param fieldName field name
     * @param <T> the result type
     * @return the result
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object obj, String fieldName) {
        Field field = getField(obj, fieldName);

        try {
            return Objects.isNull(field) ? null : (T)field.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set value to the class field
     * @param className class which the field in
     * @param fieldName field name
     * @param value the value for setting to the field
     */
    public static void setFieldValue(String className, String fieldName, Object value) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        setFieldValue(clazz, fieldName, value);
    }

    /**
     * Set value to the instance or class field
     * @param obj instance or class
     * @param fieldName field name
     * @param value the value for setting to the field
     */
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        Field field = getField(obj, fieldName);
        if (Objects.isNull(field)) {
            return;
        }

        try {
            field.set(obj, value);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * A Method wrapper for avoid NPE
     */
    public static class MethodWrapper {
        private Method method;
        private Object object;

        /**
         * A class or instance method wrapper
         * @param obj instance or class
         * @param methodName method name
         * @param parameterTypes method parameter types
         */
        public MethodWrapper(Object obj, String methodName, Class<?>... parameterTypes) {
            if (Objects.isNull(obj)) {
                return;
            }
            this.object = obj;

            Class<?> clazz = obj instanceof Class<?> ? (Class<?>)obj : obj.getClass();
            try {
                final String key = clazz.getCanonicalName() + "." + methodName;
                method = METHOD_CACHE.get(key);
                if (Objects.isNull(method)) {
                    method = clazz.getMethod(methodName, parameterTypes);
                    method.setAccessible(true);
                    METHOD_CACHE.put(key, method);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * The method invoke
         * @param args arguments
         * @param <T> the result type
         * @return invoke result
         */
        @SuppressWarnings("unchecked")
        public <T> T invoke(Object... args) {
            if (Objects.isNull(object)) {
                return null;
            }

            try {
                return (T)method.invoke(object, args);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }
}
