package org.joker.agent.factory;

import org.joker.agent.dto.TokenOverflowConfig;
import org.joker.agent.enums.TokenOverflowStrategyEnum;
import org.joker.agent.token.NoTokenOverflowStrategy;
import org.joker.agent.token.SlidingWindowTokenOverflowStrategy;
import org.joker.agent.token.SummarizeTokenOverflowStrategy;
import org.joker.agent.token.TokenOverflowStrategy;
import org.springframework.stereotype.Service;

/**
 * Token超限处理策略工厂类 根据策略类型创建对应的策略实例
 */
@Service
public class TokenOverflowStrategyFactory {

    /**
     * 根据策略类型创建对应的策略实例
     *
     * @param strategyType 策略类型
     * @param config       策略配置
     * @return 策略实例
     */
    public static TokenOverflowStrategy createStrategy(TokenOverflowStrategyEnum strategyType,
                                                       TokenOverflowConfig config) {
        if (strategyType == null) {
            return new NoTokenOverflowStrategy();
        }

        return switch (strategyType) {
            case SLIDING_WINDOW -> new SlidingWindowTokenOverflowStrategy(config);
            case SUMMARIZE -> new SummarizeTokenOverflowStrategy(config);
            default -> new NoTokenOverflowStrategy();
        };
    }

    /**
     * 根据策略名称字符串创建对应的策略实例
     *
     * @param strategyName 策略名称字符串
     * @param config       策略配置
     * @return 策略实例
     */
    public static TokenOverflowStrategy createStrategy(String strategyName, TokenOverflowConfig config) {
        TokenOverflowStrategyEnum strategyType = TokenOverflowStrategyEnum.fromString(strategyName);
        return createStrategy(strategyType, config);
    }

    /**
     * 根据配置创建对应的策略实例
     *
     * @param config 策略配置
     * @return 策略实例
     */
    public static TokenOverflowStrategy createStrategy(TokenOverflowConfig config) {
        if (config == null) {
            return new NoTokenOverflowStrategy();
        }

        return createStrategy(config.getStrategyType(), config);
    }
}
