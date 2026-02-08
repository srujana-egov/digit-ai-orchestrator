package org.digit.ai.tools.boundary;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class BoundaryConfigureTool implements ToolHandler {

    @Override
    public String name() {
        return "boundary.configure";
    }

    @Override
    public void execute(ConfigState state) {
        state.setBoundaryConfigured(true);
    }
}
