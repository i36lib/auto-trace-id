package cn.xlibs.trace.spring.support;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;

import static cn.xlibs.trace.sniffer.support.Define.*;

/**
 * A helper class for springboot
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class SpringSupport {
    private SpringSupport() {}

    /**
     * Extract the scan packages from the annotation
     * @param primarySource the primary source class
     * @return the package prefixes
     */
    public static String getInterceptPackagePrefixes(Class<?> primarySource) {
        String[] packagePrefixes = null;

        Annotation annotation = AnnotationUtils.findAnnotation(primarySource, ComponentScan.class);
        if (Objects.nonNull(annotation)) {
            packagePrefixes = (String[])AnnotationUtils.getValue(annotation, "value");
        }

        if(Objects.isNull(packagePrefixes) || packagePrefixes.length == 0){
            annotation = AnnotationUtils.findAnnotation(primarySource, SpringBootApplication.class);
            if (Objects.nonNull(annotation)) {
                packagePrefixes = (String[])AnnotationUtils.getValue(annotation, "scanBasePackages");
            }
        }

        if (Objects.nonNull(packagePrefixes)) {
            packagePrefixes = Arrays.stream(packagePrefixes).filter(p ->
                    !p.startsWith(AUTO_TRACE_PKG)).toArray(String[]::new);
            if(packagePrefixes.length > 0) {
                return String.join(COMMA, packagePrefixes);
            }
        }

        return EMPTY_STRING;
    }
}
