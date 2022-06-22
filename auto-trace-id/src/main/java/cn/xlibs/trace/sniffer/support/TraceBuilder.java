package cn.xlibs.trace.sniffer.support;

import cn.xlibs.trace.sniffer.AutoTraceId;
import cn.xlibs.trace.sniffer.interceptor.InterceptorLoader;
import cn.xlibs.trace.sniffer.spi.Interceptor;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static cn.xlibs.trace.sniffer.support.Define.*;
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
    private final Class<?> primarySource;
    private final String packagePrefixs;
    private ElementMatcher.Junction<? super TypeDescription> packagePrefixJunction;

    private TraceBuilder() {
        this(AutoTraceId.class, EMPTY_STRING);
    }

    private TraceBuilder(Class<?> primarySource, String packagePrefixs) {
        this.packagePrefixJunction = null;
        this.primarySource = primarySource;
        this.packagePrefixs = packagePrefixs;
    }

    /**
     * Get a trace builer for intercepting package prefixes
     * @param packagePrefixes the package prefixes for intercepting
     * @return A TraceBuilder instance
     */
    public static PrimarySource intercept(String packagePrefixes) {
        if (Objects.isNull(packagePrefixes) || packagePrefixes.isEmpty()) {
            packagePrefixes = EMPTY_STRING;
        }

        System.err.println("Auto trace id intercept for packages " + packagePrefixes);

        return new PrimarySource(packagePrefixes.trim());
    }

    /**
     * Specify the instrument for interceping
     * @param instrument Instrumentation
     * @throws IOException -
     */
    public final void on(Instrumentation instrument) throws IOException {
        // build the package prefix junction
        this.resolvePackagePrefixJunction();
        // inject classes into the bootloader
        instrument.appendToBootstrapClassLoaderSearch(getTraceCtxJar(primarySource));
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

    /**
     * Get the auto-trace-ctx.jar
     * @param primarySource primary source
     * @return auto-trace-ctx.jar
     * @throws IOException -
     */
    private static JarFile getTraceCtxJar(Class<?> primarySource) throws IOException {
        final String autoTraceIdPath = getSourceLocationPath(AutoTraceId.class);
        final String primarySourcePath = getSourceLocationPath(primarySource);
        final String currentJarPath = (primarySourcePath.split("\\.jar")[0]
                + JAR_SUFFIX).replace(FILE_PROTOCOL, EMPTY_STRING);

        if (primarySource != AutoTraceId.class) {
            if (autoTraceIdPath.startsWith(FILE_PROTOCOL)) {
                // auto-trace-id.jar is packed in a jar file, should be a springboot app runnable jar
                final JarFile autoTraceIdJar = extractJarFile(primarySource
                        , currentJarPath, AUTO_TRACE_ID, AUTO_TRACE_CTX, AUTO_TRACE_SPR);
                return extractJarFile(AutoTraceId.class, autoTraceIdJar, AUTO_TRACE_CTX);
            }

            // from idea, not in a jar
            return extractJarFile(AutoTraceId.class, autoTraceIdPath, AUTO_TRACE_CTX);
        }

        // from java agent, in auto-trace-id.jar
        return extractJarFile(primarySource, currentJarPath, AUTO_TRACE_CTX);
    }

    /**
     * Get the source location path
     * @param source The source
     * @return Source path
     */
    private static String getSourceLocationPath(Class<?> source) {
        return source.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    /**
     *
     * @param jarPrimarySource The primary source of the jar file
     * @param currentJarPath The current jar path of the primary source
     * @param jarEntryKey The jarEntry keyword of the jar entry we need
     * @param jarEntryFilters The exclusive jarEntry keyword of the jar entry
     * @return The jar file we need.
     * @throws IOException -
     */
    private static JarFile extractJarFile(Class<?> jarPrimarySource, String currentJarPath
            , String jarEntryKey, String... jarEntryFilters) throws IOException {
        try (JarFile jarFile = new JarFile(currentJarPath)) {
            return extractJarFile(jarPrimarySource, jarFile, jarEntryKey, jarEntryFilters);
        }
    }

    /**
     * Extract the jar file from a jar file
     * @param jarPrimarySource The primary source of the jar file
     * @param jarFile The jar file
     * @param jarEntryKey The jarEntry keyword of the jar entry we need
     * @param jarEntryFilters The exclusive jarEntry keyword of the jar entry
     * @return The jar file we need.
     * @throws IOException -
     */
    private static JarFile extractJarFile(Class<?> jarPrimarySource, JarFile jarFile
            , String jarEntryKey, String... jarEntryFilters) throws IOException {
        String jarEntryName = null;
        Enumeration<JarEntry> je = jarFile.entries();
        while (je.hasMoreElements()) {
            JarEntry jarEntry = je.nextElement();
            if (jarEntry.getName().contains(jarEntryKey) && jarEntry.getName().endsWith(JAR_SUFFIX)
                    && Stream.of(jarEntryFilters).noneMatch(m -> jarEntry.getName().contains(m)) ) {
                jarEntryName = jarEntry.getName();
                break;
            }
        }
        if (Objects.isNull(jarEntryName)) {
            throw new IllegalStateException(jarEntryKey + JAR_SUFFIX + " not found.");
        }

        InputStream jarFileInputStream = jarPrimarySource.getResourceAsStream(File.separator + jarEntryName);
        final File extractJarFile = Files.createTempFile(jarEntryKey, JAR_SUFFIX).toFile();
        copy(jarFileInputStream, new FileOutputStream(extractJarFile));

        return new JarFile(extractJarFile);
    }

    /**
     * Just a copy buffer
     * @param source A InputStream
     * @param target A OutputStream
     * @throws IOException -
     */
    private static void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    public static final class PrimarySource {
        private final String packagePrefixes;

        private PrimarySource() {
            this.packagePrefixes = EMPTY_STRING;
        }

        private PrimarySource(String packagePrefixes) {
            this.packagePrefixes = packagePrefixes;
        }

        public final TraceBuilder withPrimarySource(Class<?> primarySource) {
            return new TraceBuilder(primarySource, this.packagePrefixes);
        }
    }
}
