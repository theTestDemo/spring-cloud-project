package com.example.ai.config;

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
 * 在应用启动时加载限流规则，针对 AI 对话接口设置 QPS 阈值。
 * 当请求超过阈值时，触发降级处理（blockHandler），返回友好提示，
 * 防止突发流量导致大模型 API 调用费用暴涨或服务雪崩。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@Component
public class SentinelConfig {

    /**
     * 初始化限流规则：
     * <ul>
 *     <li>aiChat：普通对话接口，QPS 限制为 10（大模型调用成本高，需严格控制）</li>
 *     <li>aiChatWithTools：工具对话接口，QPS 限制为 5（涉及跨服务调用，耗时更长）</li>
 *     <li>aiChatStream：流式对话接口，QPS 限制为 5（长连接占用资源，需限制并发）</li>
 * </ul>
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 规则1：普通对话接口，QPS 限制为 10
        FlowRule chatRule = new FlowRule();
        chatRule.setResource("aiChat");
        chatRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        chatRule.setCount(10);
        rules.add(chatRule);

        // 规则2：工具对话接口，QPS 限制为 5（涉及跨服务调用，更耗资源）
        FlowRule chatWithToolsRule = new FlowRule();
        chatWithToolsRule.setResource("aiChatWithTools");
        chatWithToolsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        chatWithToolsRule.setCount(5);
        rules.add(chatWithToolsRule);

        // 规则3：流式对话接口，QPS 限制为 5（长连接，占用资源较多）
        FlowRule streamRule = new FlowRule();
        streamRule.setResource("aiChatStream");
        streamRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        streamRule.setCount(5);
        rules.add(streamRule);

        // 加载限流规则
        FlowRuleManager.loadRules(rules);
    }
}
