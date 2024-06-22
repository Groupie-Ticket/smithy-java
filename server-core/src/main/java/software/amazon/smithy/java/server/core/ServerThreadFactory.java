package software.amazon.smithy.java.server.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final boolean isDaemon;
    private final String namePrefix;

    public ServerThreadFactory(String namePrefix, boolean isDaemon) {
        this.isDaemon = isDaemon;
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
        t.setDaemon(isDaemon);
        return t;
    }
}
