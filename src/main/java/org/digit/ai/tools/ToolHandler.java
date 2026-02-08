package org.digit.ai.tools;

import org.digit.ai.state.ConfigState;

public interface ToolHandler {

    /**
     * Unique name of the tool.
     * Must match the name returned by AllowedToolsResolver.
     */
    String name();

    /**
     * Execute the tool and mutate configuration state.
     */
    void execute(ConfigState state);
}
