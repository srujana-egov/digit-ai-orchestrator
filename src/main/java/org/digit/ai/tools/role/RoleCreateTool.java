package org.digit.ai.tools.role;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class RoleCreateTool implements ToolHandler {

    @Override
    public String name() {
        return "role.create";
    }

    @Override
    public void execute(ConfigState state) {
        state.getRole().setCreated(true);
    }
}
