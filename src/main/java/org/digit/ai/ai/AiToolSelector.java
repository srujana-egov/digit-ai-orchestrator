package org.digit.ai.ai;

import java.util.List;

public interface AiToolSelector {
    AiDecision decide(String userMessage, List<String> allowedTools);
}
