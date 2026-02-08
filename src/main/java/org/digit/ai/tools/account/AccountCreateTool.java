package org.digit.ai.tools.account;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class AccountCreateTool implements ToolHandler {

    @Override
    public String name() {
        return "account.create";
    }

    @Override
    public void execute(ConfigState state) {
        // v1: just mark account as created
        // later: call digit-cli / DIGIT API here
        state.getAccount().setCreated(true);
    }
}
