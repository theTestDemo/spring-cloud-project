package com.example.common.util;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 生成的 ID 是 64 位 long 类型，结构：
 * 1 位符号位 + 41 位时间戳 + 10 位工作机器 ID + 12 位序列号。
 * 每毫秒可生成 4096 个 ID。
 * </p>
 * <p>
 * 注意：需要为每个服务分配唯一的 workerId 和 datacenterId。
 * 本工具类为单例，使用时需确保 workerId 唯一。
 * </p>
 *
 * @author 你的名字
 */
public class SnowflakeIdWorker {

    /** 开始时间戳 (2020-01-01 00:00:00) */
    private final long twepoch = 1577808000000L;

    /** 机器 ID 所占位数 */
    private final long workerIdBits = 5L;
    /** 数据中心 ID 所占位数 */
    private final long datacenterIdBits = 5L;
    /** 支持的最大机器 ID，结果是 31 */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    /** 支持的最大数据中心 ID，结果是 31 */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    /** 序列号所占位数 */
    private final long sequenceBits = 12L;

    /** 机器 ID 左移位数 */
    private final long workerIdShift = sequenceBits;
    /** 数据中心 ID 左移位数 */
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    /** 时间戳左移位数 */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    /** 序列号掩码 */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造器
     * @param workerId     机器 ID (0~31)
     * @param datacenterId 数据中心 ID (0~31)
     */
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId 不能大于 %d 或小于 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenterId 不能大于 %d 或小于 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获取下一个 ID
     * @return 唯一 ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 时钟回拨处理
        if (timestamp < lastTimestamp) {
            // 发生时钟回拨，可以抛出异常或等待
            throw new RuntimeException(String.format("时钟回拨，拒绝生成ID。上次时间戳 %d，当前时间戳 %d", lastTimestamp, timestamp));
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 同一毫秒内序列号用完，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    /**
     * 等待下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前毫秒数
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}
