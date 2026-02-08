package org.digit.ai.ai;

public record AiDecision(
        DecisionType type,
        String tool,
        String message,
        String proposedAction
) {
    public enum DecisionType {
        EXECUTE,
        EXPLAIN
    }

    public static AiDecision execute(String tool) {
        return new AiDecision(DecisionType.EXECUTE, tool, null, null);
    }

    public static AiDecision explain(String message) {
        return new AiDecision(DecisionType.EXPLAIN, null, message, null);
    }

    public static AiDecision explain(String message, String proposedAction) {
        return new AiDecision(DecisionType.EXPLAIN, null, message, proposedAction);
    }
}
