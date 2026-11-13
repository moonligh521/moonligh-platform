package com.moonligh.consult.framework.redis;

import java.util.function.Supplier;

/**
 * 同一业务键同一时刻只跑一个重操作（连点 / 超时重试挡第二趟）。
 * 正确性仍由下游行锁等保证；守卫失效只会多花资源，不会写错数据。
 */
public interface ExclusiveGuard {

    ExclusiveGuard NONE = new ExclusiveGuard() {
        @Override
        public <T> T exclusive(String key, Supplier<T> action) {
            return action.get();
        }
    };

    /**
     * 拿不到锁时抛业务异常（由实现配置 ErrorCode）；key 为空则直接执行。
     */
    <T> T exclusive(String key, Supplier<T> action);
}
