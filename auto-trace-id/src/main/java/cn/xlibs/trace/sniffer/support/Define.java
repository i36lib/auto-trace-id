package cn.xlibs.trace.sniffer.support;

/**
 * The constants definition
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class Define {
    private Define() {}

    /**
     * The trace id name in the log
     */
    public static final String TRACE_KEY = "autoTraceId";
    /**
     * The http header trace key
     */
    public static final String TRACE_HEADER = "X-AUTO-TRACE-ID";

    public static final String COMMA = ",";
    public static final String HYPHEN = "-";
    public static final String EMPTY_STRING = "";
    public static final String JAR_SUFFIX = ".jar";
    public static final String FILE_PROTOCOL = "file:";
    public static final String JAVA_AGENT_INSTRUCTION = "-javaagent:";

    public static final String AUTO_TRACE_ID = "auto-trace-id";
    public static final String AUTO_TRACE_CTX = "auto-trace-ctx";
    public static final String AUTO_TRACE_SPR = "auto-trace-spring";

    public static final String AUTO_TRACE_PKG = "cn.xlibs.trace";
    public static final String INTELLIJ_RT =  "com.intellij.rt.";
    public static final String SPRING_BOOT_DEV_TOOLS = "org.springframework.boot.devtools";
    public static final String SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication";
}
