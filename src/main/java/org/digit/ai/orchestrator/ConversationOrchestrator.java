package org.digit.ai.orchestrator;

import org.digit.ai.gating.AllowedToolsResolver;
import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

import java.util.List;

public class ConversationOrchestrator {

    private final AllowedToolsResolver resolver;
    private final ToolRegistry toolRegistry;

    public ConversationOrchestrator(
            AllowedToolsResolver resolver,
            ToolRegistry toolRegistry
    ) {
        this.resolver = resolver;
        this.toolRegistry = toolRegistry;
    }

    public void execute(String toolName, ConfigState state) {
        List<String> allowedTools = resolver.resolve(state);

        if (!allowedTools.contains(toolName)) {
            throw new IllegalStateException(
                "Tool not allowed in current state: " + toolName
            );
        }

        ToolHandler tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        tool.execute(state);
    }

    public List<String> getAllowedTools(ConfigState state) {
    return resolver.resolve(state);
}

}
