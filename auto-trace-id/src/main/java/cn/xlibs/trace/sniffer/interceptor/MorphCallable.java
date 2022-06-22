package cn.xlibs.trace.sniffer.interceptor;

/**
 * A morph callable for modifying the origin method
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public interface MorphCallable {
    /**
     * The original method call
     * @param args Arguments of the original method
     * @return The result of the original method call
     */
    Object call(Object[] args);
}
