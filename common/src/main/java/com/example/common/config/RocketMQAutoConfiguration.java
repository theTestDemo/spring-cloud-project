package com.example.common.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 自动配置类
 * <p>
 * 提供 RocketMQTemplate 的默认 Bean，供各微服务模块使用
 * 当 classpath 中存在 RocketMQTemplate 类时才生效（即已引入 rocketmq-spring-boot-starter 依赖）
 * 利用{@link ConditionalOnMissingBean} 允许业务模块自定义覆盖
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Configuration
@ConditionalOnClass(RocketMQTemplate.class)
public class RocketMQAutoConfiguration {
    /**
     * 创建 RocketMQTemplate Bean
     * <p>
     * 只有当容器中没有 RocketMQTemplage 类型的 Bean 时才会执行
     * Spring Boot 会自动加载 application.yml 中的 rocketmq 配置（如 name-server、producer group）
     * </p>
     *
     * @return RocketMQTemplate实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
}