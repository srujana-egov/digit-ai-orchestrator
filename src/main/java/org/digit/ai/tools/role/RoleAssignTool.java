package org.digit.ai.tools.role;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class RoleAssignTool implements ToolHandler {

    @Override
    public String name() {
        return "role.assign";
    }

    @Override
    public void execute(ConfigState state) {
        state.setRoleAssignmentDone(true);
    }
}
