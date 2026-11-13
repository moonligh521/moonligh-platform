package com.moonligh.consult.framework.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moonligh.consult.framework.BusinessException;
import com.moonligh.consult.framework.ErrorCode;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Redisson 分布式单飞锁。不等待：拿不到说明同键已有请求在跑。
 * 租期固定、不续期；Redis 不可用时放行，退回下游行锁串行。
 */
public class RedissonExclusiveGuard implements ExclusiveGuard {

    private static final Logger log = LoggerFactory.getLogger(RedissonExclusiveGuard.class);

    private final RedissonClient redisson;
    private final String keyPrefix;
    private final long leaseMs;
    private final ErrorCode busyCode;
    private final String metricName;
    private final MeterRegistry meters;

    public RedissonExclusiveGuard(
            RedissonClient redisson,
            String keyPrefix,
            Duration lease,
            ErrorCode busyCode,
            String metricName,
            MeterRegistry meters) {
        this.redisson = redisson;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
        this.leaseMs = lease.toMillis();
        this.busyCode = busyCode;
        this.metricName = metricName == null || metricName.isBlank() ? "exclusive_guard_total" : metricName;
        this.meters = meters;
    }

    @Override
    public <T> T exclusive(String key, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        RLock lock;
        boolean locked;
        try {
            lock = redisson.getLock(keyPrefix + key);
            locked = lock.tryLock(0, leaseMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BusinessException(busyCode);
        } catch (RuntimeException error) {
            log.warn("单飞锁不可用，退回行锁串行 type={}", error.getClass().getSimpleName());
            count("bypass");
            return action.get();
        }
        if (!locked) {
            count("rejected");
            throw new BusinessException(busyCode);
        }
        count("acquired");
        try {
            return action.get();
        } finally {
            unlock(lock);
        }
    }

    private void unlock(RLock lock) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (RuntimeException error) {
            log.warn("释放单飞锁失败，等租期到期 type={}", error.getClass().getSimpleName());
        }
    }

    private void count(String outcome) {
        if (meters != null) {
            meters.counter(metricName, "outcome", outcome).increment();
        }
    }
}
