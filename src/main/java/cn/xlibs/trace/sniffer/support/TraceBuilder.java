package cn.xlibs.trace.sniffer.support;

import cn.xlibs.trace.sniffer.AutoTraceId;
import cn.xlibs.trace.sniffer.context.TraceId;
import cn.xlibs.trace.sniffer.interceptor.InterceptorLoader;
import cn.xlibs.trace.sniffer.spi.Interceptor;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * The auto trace agent builder
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class TraceBuilder {
    private final File temporaryFile;
    private final String packagePrefixs;
    private final RandomString randomString = new RandomString();
    private ElementMatcher.Junction<? super TypeDescription> packagePrefixJunction;

    /**
     * Dont allow to new the TraceBuilder outside
     */
    private TraceBuilder() throws IOException {
        this(Define.EMPTY_STRING);
    }

    /**
     * Dont allow to new the TraceBuilder outside
     */
    private TraceBuilder(String packagePrefixs) throws IOException {
        this.packagePrefixJunction = null;
        this.packagePrefixs = packagePrefixs.trim();
        this.temporaryFile = Files.createTempDirectory(Define.AUTO_TRACE_ID).toFile();
    }

    /**
     * Get a trace builer for intercepting package prefixes
     * @param packagePrefixes the package prefixes for intercepting
     * @return A TraceBuilder instance
     */
    public static TraceBuilder intercept(String packagePrefixes) throws IOException {
        if (Objects.isNull(packagePrefixes) || packagePrefixes.isEmpty()) {
            packagePrefixes = Define.EMPTY_STRING;
        }

        System.err.println("Auto trace id intercept for packages " + packagePrefixes);

        return new TraceBuilder(packagePrefixes.trim());
    }

    /**
     * Specify the instrument for interceping
     * @param instrument Instrumentation
     */
    public final void on(Instrumentation instrument) {
        // build the package prefix junction
        this.resolvePackagePrefixJunction();
        // inject classes into the bootloader
        this.injectClassIntoBootLoader(instrument);
        // get a bytebuddy agent builder
        AgentBuilder builder = this.newAgentBuilder(instrument);
        // do the transformation with instrument
        this.transformation(builder).installOn(instrument);
    }

    /**
     * Try to get the instrumentation from the system class loader.
     * Install the bytebuddy agent to get one if there is none.
     * @return the Instrumentation instance
     */
    public static Instrumentation getInstrumentation() {
        try {
            return ByteBuddyAgent.getInstrumentation();
        } catch (Exception e) {
            return ByteBuddyAgent.install();
        }
    }

    /**
     * Do the transformation with the agent builder
     * @param agentBuilder the AgentBuilder
     */
    private AgentBuilder transformation(AgentBuilder agentBuilder) {
        for (Interceptor interceptor : InterceptorLoader.load()) {
            // Continue if the type is not resolved or there is not type or method matcher
            if (!interceptor.isTypeResolved() || Objects.isNull(interceptor.typeMatcher())
                    || Objects.isNull(interceptor.methodMatcher())) {
                continue;
            }

            // transform and get a new builder
            agentBuilder = agentBuilder.type(interceptor.typeMatcher())
                    .transform((builder, typeDescription, classLoader, module) -> {
                        // transform the type if needed
                        DynamicType.Builder<?> newBuilder = interceptor.transformType(builder
                                , typeDescription, classLoader);
                        if (Objects.isNull(newBuilder)) {
                            newBuilder = builder;
                        }

                        // TODO: transtormation

                        return newBuilder;
                    });
        }
        return agentBuilder;
    }

    /**
     * New a bytebuddy AgentBuilder with instrument
     * @param instrument the instrument
     * @return A bytebuddy AgentBuilder
     */
    private AgentBuilder newAgentBuilder(Instrumentation instrument) {
        return new AgentBuilder.Default()
                .ignore(this.buildBytebuddyIgnore())
                .ignore(nameStartsWith(Define.INTELLIJ_RT)
                        .or(nameStartsWith(Define.SPRING_BOOT_DEV_TOOLS))
                        .or(isAnnotatedWith(named(Define.SPRING_BOOT_APPLICATION)))
                        .or(nameStartsWith(AutoTraceId.class.getPackage().getName())))
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.InjectionStrategy.UsingInstrumentation(instrument, this.temporaryFile))
                .with(net.bytebuddy.agent.builder.AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .with(net.bytebuddy.agent.builder.AgentBuilder.InstallationListener.StreamWriting.toSystemError());
    }

    /**
     * build the ignores elementmacher
     * @return The ignores elementmacher
     */
    private ElementMatcher<? super TypeDescription> buildBytebuddyIgnore() {
        ElementMatcher.Junction<? super TypeDescription> junction = any();
        for (Interceptor interceptor : InterceptorLoader.load()) {
            if (!interceptor.isTypeResolved()) {
                continue;
            }
            junction = junction.and(not(interceptor.typeMatcher()));
        }

        return junction;
    }

    /**
     * Inject the class into the boot loader for intercept the boot class
     * @param instrument the Instrumentation
     */
    private void injectClassIntoBootLoader(Instrumentation instrument) {
        /* We need to inject the TraceId and DecoratedTask class
         * into the bootloader as we will intercept the ThreadPoolExecutor. */
        List<Class<?>> injectClassList = Arrays.asList(TraceId.class, DecoratedTask.class);

        Map<TypeDescription.ForLoadedType, byte[]> injectTypesMap = new HashMap<>(2);
        for (Class<?> clazz : injectClassList) {
            injectTypesMap.put(new TypeDescription.ForLoadedType(clazz),
                    ClassFileLocator.ForClassLoader.read(clazz)
            );
        }

        Map<String, byte[]> binaryRepresentations = new LinkedHashMap<>();
        for (Map.Entry<? extends TypeDescription, byte[]> entry : injectTypesMap.entrySet()) {
            binaryRepresentations.put(entry.getKey().getName() + randomString.nextString(), entry.getValue());
        }

        /* ClassInjector.UsingUnsafe.ofBootLoader().injectRaw(binaryRepresentations); */
        ClassInjector.UsingInstrumentation.of(this.temporaryFile, ClassInjector.UsingInstrumentation
                .Target.BOOTSTRAP, instrument).injectRaw(binaryRepresentations);
    }

    /**
     * Resolve and build the Package Prefix Junction
     */
    private void resolvePackagePrefixJunction() {
        for (String prefix : this.packagePrefixs.split(Define.COMMA)) {
            if (Objects.isNull(this.packagePrefixJunction)) {
                this.packagePrefixJunction = nameStartsWith(prefix);
                continue;
            }

            this.packagePrefixJunction = this.packagePrefixJunction
                    .or(nameStartsWith(prefix));
        }
    }
}
