package com.tvd12.ezyfoxserver.socket;

import lombok.Setter;

public abstract class EzySimpleSocketEventLoop extends EzySocketEventLoop {

    @Setter
    protected String threadName;
    @Setter
    protected int threadPoolSize;
    
    @Override
    protected String threadName() {
        return threadName;
    }

    @Override
    protected int threadPoolSize() {
        return threadPoolSize;
    }
    
    protected final void eventLoop() {
        getLogger().info("{} event loop has started", currentThreadName());
        eventLoop0();
        getLogger().info("{} event loop has stopped", currentThreadName());
    }
    
    protected abstract void eventLoop0();
    
    private String currentThreadName() {
        return Thread.currentThread().getName();
    }
}
