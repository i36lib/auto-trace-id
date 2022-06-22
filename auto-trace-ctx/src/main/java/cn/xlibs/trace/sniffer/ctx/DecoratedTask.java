package cn.xlibs.trace.sniffer.ctx;

import java.util.Objects;

/**
 * The decorated task for intercepting ThreadPoolExecutor
 *
 * @author 阿北
 * @since 1.0.0
 * <p>
 * All rights Reserved.
 */
public final class DecoratedTask implements Runnable {
    private final String traceId;
    private final Runnable rawTask;

    /**
     * Use the parameterized one instead
     */
    private DecoratedTask() {
        this(null, null);
    }

    /**
     * DecoratedTask with the raw task and traceId
     * @param rawTask the raw runnable task
     * @param traceId the traceId
     */
    public DecoratedTask(Runnable rawTask, String traceId) {
        this.rawTask = rawTask;
        this.traceId = traceId;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (Objects.isNull(this.rawTask)) {
            // Impossible
            return;
        }

        try {
            if (Objects.nonNull(traceId)) {
                TraceId.set(traceId);
            }
            this.rawTask.run();
        } finally {
            TraceId.remove();
        }
    }
}
