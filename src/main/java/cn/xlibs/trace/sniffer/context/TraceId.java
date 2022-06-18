package cn.xlibs.trace.sniffer.context;

import cn.xlibs.trace.sniffer.support.Define;

import java.util.UUID;

/**
 * The trace context
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class TraceId {
    private TraceId(){}

    /**
     * The context for handle the trace id
     */
    private static final ThreadLocal<String> TRACE_ID_CTX = new ThreadLocal<>();

    /**
     * remove trace id from current thread context
     */
    public static void remove() {
        TRACE_ID_CTX.remove();
    }

    /**
     * Get trace id from current thread context
     * @return trace id
     */
    public static String get() {
        return TRACE_ID_CTX.get();
    }

    /**
     * Set trace id to current thread context
     * @param traceId the trace id
     */
    public static void set(String traceId) {
        TRACE_ID_CTX.set(traceId);
    }

    /**
     * Generate a trace id base on UUID
     * @return The generated trace id
     */
    public static String generate() {
        return UUID.randomUUID()
                .toString()
                .substring(18)
                .replace(Define.HYPHEN, Define.EMPTY_STRING);
    }
}
