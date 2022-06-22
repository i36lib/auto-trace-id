package cn.xlibs.trace.sniffer.spi;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Bytecode interceptor
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public interface Interceptor {
    /**
     * If the type resolved
     * @return true if resolved
     */
    default boolean isTypeResolved() {
        return true;
    }

    /**
     * Get the interceptor type matcher
     * @return The interceptor type matcher
     */
    ElementMatcher<? super TypeDescription> typeMatcher();

    /**
     * Get the interceptor method matcher
     * @return The interceptor method matcher
     */
    ElementMatcher<? super MethodDescription> methodMatcher();

    /**
     * Transform the type if needed
     * @param builder the bytebuddy agent builder
     * @param typeDescription the type description
     * @param classLoader current ClassLoader
     * @return the new builder
     */
    default DynamicType.Builder<?> transformType(DynamicType.Builder<?> builder
            , TypeDescription typeDescription, ClassLoader classLoader) {
        return builder;
    }
}
