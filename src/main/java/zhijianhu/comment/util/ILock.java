package zhijianhu.comment.util;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/21
 * 说明:
 */
public interface ILock {
    /**
     * @param timeoutSec 锁过期时间
     * @return 是否获取锁成功
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();
}
