package cn.xlibs.trace.sniffer.interceptor;

import cn.xlibs.trace.sniffer.spi.Interceptor;

import java.util.Collections;
import java.util.List;

/**
 * The Interceptor Loader
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class InterceptorLoader {
    private InterceptorLoader() {}

    public static List<Interceptor> load() {
        return Collections.emptyList();
    }
}
