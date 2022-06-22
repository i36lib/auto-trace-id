package cn.xlibs.trace.spring;

import cn.xlibs.trace.sniffer.support.TraceBuilder;
import cn.xlibs.trace.spring.support.SpringSupport;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.xlibs.trace.sniffer.support.Define.*;

/**
 * The auto trace
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class AutoTrace {
    private AutoTrace() {}

    /**
     * Hook with the main entry arguments
     * @param args the main entry arguments
     * @return A TraceHook
     */
    public static TraceHook hookWith(String[] args) {
        return new TraceHook(args);
    }

    /**
     * A Trace Hook
     */
    public static final class TraceHook {
        private final String[] args;
        private static boolean hooked = false;

        /**
         * Dont allow to new the TraceHook outside
         */
        private TraceHook(){
            this(null);
        }

        /**
         * Dont allow to new the TraceHook outside
         * @param args the main entry arguments
         */
        private TraceHook(String[] args) {
            this.args = Objects.isNull(args) ? new String[]{} : args;
        }

        /**
         * Call before SpringBootApplication.run(primarySource);
         * @param primarySource the spring boot application entry class
         */
        public void beforeRunning(Class<?> primarySource) {
            try {
                if(hooked) { return; }
                markHooked();

                System.err.println("Registering auto trace id hook with args " + Arrays.asList(this.args)
                        + " before running springboot application " + primarySource.getName());

                List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
                boolean runningWithAgent = inputArguments.stream().anyMatch(f -> f
                        .startsWith(JAVA_AGENT_INSTRUCTION) && f.contains(AUTO_TRACE_ID));
                if (runningWithAgent){
                    System.err.println("Auto trace id has already hooked in java agent mode");
                    return;
                }

                final String packagePrefixes = SpringSupport.getInterceptPackagePrefixes(primarySource);

                TraceBuilder.intercept(packagePrefixes).on(primarySource, TraceBuilder.getInstrumentation());
            } catch (Throwable e) {
                System.err.println("Failed to register auto trace id hook: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private static void markHooked() { hooked = true; }
    }
}
