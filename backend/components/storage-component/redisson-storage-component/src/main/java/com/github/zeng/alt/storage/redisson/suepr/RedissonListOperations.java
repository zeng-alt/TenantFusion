package com.github.zeng.alt.storage.redisson.suepr;

import com.github.zeng.alt.storage.CacheListOperations;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redisson List 结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonListOperations implements CacheListOperations {

    private final RedissonClient redissonClient;

    public RedissonListOperations(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    

    @Override
    public Long leftPush(String key, String value) {
        RList<String> list = redissonClient.getList(key);
        list.addFirst(value);
        return (long) list.size();
    }

    @Override
    public Long leftPushAll(String key, String... values) {
        RList<String> list = redissonClient.getList(key);
        list.addAll(0, Arrays.asList(values));
        return (long) list.size();
    }

    @Override
    public Long rightPush(String key, String value) {
        RList<String> list = redissonClient.getList(key);
        list.add(value);
        return (long) list.size();
    }

    @Override
    public Long rightPushAll(String key, String... values) {
        RList<String> list = redissonClient.getList(key);
        list.addAll(Arrays.asList(values));
        return (long) list.size();
    }

    @Override
    public String leftPop(String key) {
        RList<String> list = redissonClient.getList(key);
        return list.isEmpty() ? null : list.removeFirst();
    }

    @Override
    public String leftPop(String key, long timeout, TimeUnit unit) {
        try {
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(key);
            return blockingQueue.poll(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public String rightPop(String key) {
        RList<String> list = redissonClient.getList(key);
        return list.isEmpty() ? null : list.removeLast();
    }

    @Override
    public String rightPop(String key, long timeout, TimeUnit unit) {
        try {
            RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(key);
            return blockingDeque.pollLast(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public List<String> range(String key, long start, long end) {
        RList<String> list = redissonClient.getList(key);
        return list.range((int) start, (int) end);
    }

    @Override
    public Long size(String key) {
        return (long) redissonClient.getList(key).size();
    }

    @Override
    public String index(String key, long index) {
        RList<String> list = redissonClient.getList(key);
        return list.get((int) index);
    }

    @Override
    public void set(String key, long index, String value) {
        redissonClient.getList(key).set((int) index, value);
    }

    @Override
    public Long remove(String key, long count, String value) {
        RList<String> list = redissonClient.getList(key);
        long removed = 0;
        // count > 0: remove from left, count < 0: remove from right, count == 0: remove all
        if (count >= 0) {
            for (int i = 0; i < list.size(); i++) {
                if (count > 0 && removed >= count) break;
                if (value.equals(list.get(i))) {
                    list.remove(i);
                    i--;
                    removed++;
                }
            }
        } else {
            long absCount = -count;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (removed >= absCount) break;
                if (value.equals(list.get(i))) {
                    list.remove(i);
                    removed++;
                }
            }
        }
        return removed;
    }

    @Override
    public void trim(String key, long start, long end) {
        RList<String> list = redissonClient.getList(key);
        List<String> range = list.range((int) start, (int) end);
        list.delete();
        list.addAll(range);
    }
}
