package org.joker.agent.enums;


/** 执行阶段枚举 用于标识Agent执行过程中的不同阶段 */
public enum ExecutionPhase {

    /** 初始化阶段 */
    INITIALIZATION("INITIALIZATION", "初始化阶段"),

    /** 环境准备阶段 */
    ENVIRONMENT_PREPARATION("ENVIRONMENT_PREPARATION", "环境准备阶段"),

    /** 内存初始化阶段 */
    MEMORY_INITIALIZATION("MEMORY_INITIALIZATION", "内存初始化阶段"),

    /** 模型调用阶段 */
    MODEL_CALL("MODEL_CALL", "模型调用阶段"),

    /** 工具执行阶段 */
    TOOL_EXECUTION("TOOL_EXECUTION", "工具执行阶段"),

    /** 结果处理阶段 */
    RESULT_PROCESSING("RESULT_PROCESSING", "结果处理阶段");

    private final String code;
    private final String description;

    ExecutionPhase(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据代码获取枚举值 */
    public static ExecutionPhase fromCode(String code) {
        for (ExecutionPhase phase : values()) {
            if (phase.code.equals(code)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("未知的执行阶段: " + code);
    }
}
