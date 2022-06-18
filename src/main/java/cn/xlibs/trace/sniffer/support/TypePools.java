package cn.xlibs.trace.sniffer.support;

/**
 * The class type pools of the system classloader
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class TypePools {
    private TypePools(){}

    /**
     * Get the TypePool of the BootLoader
     * @return the TypePool of BootLoader
     */
    public static net.bytebuddy.pool.TypePool getBootLoaderPool() {
        return net.bytebuddy.pool.TypePool.Default.ofBootLoader();
    }

    /**
     * Get the TypePool of the PlatformLoader
     * @return the TypePool of PlatformLoader
     */
    public static net.bytebuddy.pool.TypePool getPlatformLoaderPool() {
        return net.bytebuddy.pool.TypePool.Default.ofPlatformLoader();
    }

    /**
     * Get the TypePool of the SystemLoader
     * @return the TypePool of SystemLoader
     */
    public static net.bytebuddy.pool.TypePool getSystemLoaderPool() {
        return net.bytebuddy.pool.TypePool.Default.ofSystemLoader();
    }
}
