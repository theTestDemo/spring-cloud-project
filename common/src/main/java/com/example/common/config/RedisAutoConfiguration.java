package com.example.common.config;

import com.example.common.util.RedisUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    //只有当容器中还没有这个类型的 Bean 时，才会执行这个方法并注册。
    //用于用户客制化，假如用户在对应模块中自定义了redis，则会以用户自定义优先，如果没有客制化，则按照该注解的默认方法来
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
        /*RedisConnectionFactory：redis工厂类，帮你定义好一个连接工厂，它知道如何根据目标模块的配置文件连接你的redis
        StringRedisTemplate:Spring Data Redis定义的一个工具类，用来进行redis中的字符串数据操作，将RedisConnectionFactory
        创建好的连接作为参数传入，即可返回一个可以工作的StringRedisTemplate*/
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisUtil redisUtil(StringRedisTemplate stringRedisTemplate) {
        return new RedisUtil(stringRedisTemplate);
        /*Spring 启动时，并不是直接使用你写的 RedisAutoConfiguration 对象，而是创建一个它的 代理子类（通过 CGLIB 动态生成）。
        当 Spring 需要调用 redisUtil() 方法时，它发现参数 StringRedisTemplate stringRedisTemplate 是一个需要注入的依赖。
        Spring 会先查找容器中已经存在的 StringRedisTemplate 类型的 Bean（该 Bean 由 stringRedisTemplate() 方法创建并已注册到容器中），然后把这个 Bean 作为参数传入 redisUtil() 方法。
        关键点：redisUtil() 方法并没有直接调用 stringRedisTemplate() 方法，而是通过参数注入拿到了那个 Bean。
        即使你强行在 redisUtil() 方法体内写 stringRedisTemplate()，由于代理的存在，这个调用也会被拦截，返回容器中的单例，而不是重新执行方法体。*/
    }
}