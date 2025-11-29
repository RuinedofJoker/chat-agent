package org.joker.agent.dto;

/**
 * 模型调用信息值对象 封装单次模型调用的详细信息
 */
public class ModelCallInfo {

    /**
     * 模型部署名称
     */
    private final String modelEndpoint;

    /**
     * 输入Token数
     */
    private final Integer inputTokens;

    /**
     * 输出Token数
     */
    private final Integer outputTokens;

    /**
     * 调用耗时(毫秒)
     */
    private final Integer callTime;

    /**
     * 是否成功
     */
    private final Boolean success;

    /**
     * 错误信息
     */
    private final String errorMessage;


    private ModelCallInfo(Builder builder) {
        this.modelEndpoint = builder.modelEndpoint;
        this.inputTokens = builder.inputTokens;
        this.outputTokens = builder.outputTokens;
        this.callTime = builder.callTime;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String modelEndpoint;
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer callTime;
        private Boolean success = true;
        private String errorMessage;

        public Builder modelEndpoint(String modelEndpoint) {
            this.modelEndpoint = modelEndpoint;
            return this;
        }

        public Builder inputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }

        public Builder outputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }

        public Builder callTime(Integer callTime) {
            this.callTime = callTime;
            return this;
        }

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ModelCallInfo build() {
            return new ModelCallInfo(this);
        }
    }

    // Getter方法
    public String getModelEndpoint() {
        return modelEndpoint;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Integer getCallTime() {
        return callTime;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取总Token数
     */
    public Integer getTotalTokens() {
        if (inputTokens == null || outputTokens == null) {
            return null;
        }
        return inputTokens + outputTokens;
    }
}
