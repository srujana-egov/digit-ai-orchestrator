package org.digit.ai.tools.registry;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class RegistryConfigureTool implements ToolHandler {

    @Override
    public String name() {
        return "registry.configure";
    }

    @Override
    public void execute(ConfigState state) {
        state.setRegistrySchemaConfigured(true);
    }
}
