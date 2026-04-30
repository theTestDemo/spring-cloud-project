package com.example.goodservice.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流规则配置
 * <p>
 * 在应用启动时加载限流规则，针对商品服务的扣库存接口（reduceStock）设置 QPS 限制
 * 当请求超过阈值时，触发降级处理（返回”系统繁忙“提示），防止突发流量压垮系统
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-24
 */
@Component
public class SentinelConfig {
    /**
     * 初始化限流规则
     * <p>
     * 为资源 ”reduceStock“ 配置 QPS 限流，阈值10次/s
     * </p>
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("reduceStock");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(10); // 每秒 10 次
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}