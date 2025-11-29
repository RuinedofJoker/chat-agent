package org.joker.agent.message;

import dev.langchain4j.service.TokenStream;

public interface Agent {
    TokenStream chat(String message);
}
