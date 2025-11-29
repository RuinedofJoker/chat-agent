package org.joker.agent.enums;

/**
 * 模型调用协议
 */
public enum ProviderProtocol {

    OPENAI, ANTHROPIC;

    public static ProviderProtocol fromCode(String code) {
        for (ProviderProtocol protocol : values()) {
            if (protocol.name().equals(code)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Unknown model type code: " + code);
    }
}
