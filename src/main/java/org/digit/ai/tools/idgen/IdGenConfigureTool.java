package org.digit.ai.tools.idgen;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class IdGenConfigureTool implements ToolHandler {

    @Override
    public String name() {
        return "idgen.configure";
    }

    @Override
    public void execute(ConfigState state) {
        state.setIdGenConfigured(true);
    }
}
