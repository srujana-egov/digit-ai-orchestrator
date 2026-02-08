package org.digit.ai.tools.user;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class UserCreateTool implements ToolHandler {

    @Override
    public String name() {
        return "user.create";
    }

    @Override
    public void execute(ConfigState state) {
        state.getUser().setCreated(true);
    }
}
