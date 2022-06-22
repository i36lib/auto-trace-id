package cn.xlibs.trace.sniffer;

import cn.xlibs.trace.sniffer.support.TraceBuilder;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Objects;

/**
 * The auto trace id agent entrance
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class AutoTraceId {
    private AutoTraceId(){}

    /**
     * The agentmain entrance when using the attach api
     * @param packagePrefixes Intercept classes under these packages
     * @param instrument the Instrumentation
     */
    public static void agentmain(String packagePrefixes, Instrumentation instrument) throws IOException {
        premain(packagePrefixes, instrument);
    }

    /**
     * The agentmain entrance when using the attach api
     * @param packagePrefixes Intercept classes under these packages
     * @param instrument the Instrumentation
     */
    public static void premain(String packagePrefixes, Instrumentation instrument) throws IOException {
        if (Objects.isNull(packagePrefixes) || packagePrefixes.isEmpty()) {
            System.err.println("It is recommended to set the agent parameter to specify the package prefixes " +
                    "for intercepting to narrow the enhanced scope and speed up the startup. e.g: \n" +
                    "-javaagent:/dir/to/auto-trace-id.jar=com.example.package,org.example.package");
        }
        TraceBuilder.intercept(packagePrefixes).withPrimarySource(AutoTraceId.class).on(instrument);
    }
}
