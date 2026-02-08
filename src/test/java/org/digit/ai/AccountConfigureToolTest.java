package org.digit.ai;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.account.AccountConfigureTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountConfigureToolTest {

    @Test
    public void shouldMarkAccountAsConfiguredAndSetToken() {
        ConfigState state = new ConfigState();
        state.getAccount().setCreated(true);

        AccountConfigureTool tool = new AccountConfigureTool();
        tool.execute(state);

        assertThat(state.getAccount().isConfigured()).isTrue();
        assertThat(state.getAccount().getAccessToken()).isNotBlank();
    }
}
