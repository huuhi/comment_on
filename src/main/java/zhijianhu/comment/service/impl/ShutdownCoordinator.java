package zhijianhu.comment.service.impl;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class ShutdownCoordinator {
    private volatile boolean shutdownFlag = false;
    
    public boolean isShutdown() {
        return shutdownFlag;
    }
    
    @PreDestroy
    public void prepareShutdown() {
        shutdownFlag = true;
    }
}