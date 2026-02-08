package org.digit.ai;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.account.AccountCreateTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountCreateToolTest {

    @Test
    public void shouldMarkAccountAsCreated() {
        ConfigState state = new ConfigState();
        AccountCreateTool tool = new AccountCreateTool();

        tool.execute(state);

        assertThat(state.getAccount().isCreated()).isTrue();
    }
}
