package org.digit.ai.tools.account;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class AccountConfigureTool implements ToolHandler {

    @Override
    public String name() {
        return "account.configure";
    }

    @Override
    public void execute(ConfigState state) {
        // v1: simulate successful configuration
        state.getAccount().setConfigured(true);
        state.getAccount().setAccessToken("dummy-access-token");
    }
}
