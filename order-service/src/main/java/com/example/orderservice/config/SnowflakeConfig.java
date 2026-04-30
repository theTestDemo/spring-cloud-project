package com.example.orderservice.config;


import com.example.common.util.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法 ID 生成器配置类
 * <p>
 * 从配置文件中读取机器ID和数据中心ID，创建{@link SnowflakeIdWorker} Bean,
 * 用于生成全局唯一的订单号（长整型或字符串）
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-25
 */
@Configuration
public class SnowflakeConfig {

    @Value("${snowflake.worker-id:1}")
    private long workerId;

    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;

    /**
     * 创建雪花算法实例
     * @return SnowflakeIdWorker 实例
     */
    @Bean
    public SnowflakeIdWorker snowflakeIdWorker() {
        return new SnowflakeIdWorker(workerId, datacenterId);
    }
}
