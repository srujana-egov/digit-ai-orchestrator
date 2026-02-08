package org.digit.ai.tools.workflow;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class WorkflowConfigureTool implements ToolHandler {

    @Override
    public String name() {
        return "workflow.configure";
    }

    @Override
    public void execute(ConfigState state) {
        state.setWorkflowConfigured(true);
    }
}
