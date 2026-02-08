package org.digit.ai.orchestrator;

import org.digit.ai.tools.ToolHandler;

import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, ToolHandler> tools = new HashMap<>();

    public ToolRegistry(Iterable<ToolHandler> handlers) {
        for (ToolHandler handler : handlers) {
            tools.put(handler.name(), handler);
        }
    }

    public ToolHandler get(String toolName) {
        return tools.get(toolName);
    }
}
