package com.example.orderservice.config;

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
 * 在应用启动时加载限流规则，针对不同接口设置 QPS 阈值
 * 当请求超过阈值时，触发降级处理（blockHandler）
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@Component
public class SentinelConfig {
    /**
     * 初始化限流规则：
     * - buyGoods：普通下单接口，QPS 限制为10
     * - secKill：秒杀接口，QPS限制为5（更严格）
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        //规则1:普通下单接口
        FlowRule buyGoodsRule  = new FlowRule();
        buyGoodsRule .setResource("buyGoods");
        buyGoodsRule .setGrade(RuleConstant.FLOW_GRADE_QPS);
        buyGoodsRule .setCount(10); // 每秒 10 次
        rules.add(buyGoodsRule );
        //规则2：秒杀接口（限流更严格）
        FlowRule secKillRule = new FlowRule();
        secKillRule.setResource("secKill");
        secKillRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        secKillRule.setCount(5);
        rules.add(secKillRule);

        //加载限流规则
        FlowRuleManager.loadRules(rules);
    }
}