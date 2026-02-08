package org.digit.ai;

import org.digit.ai.gating.AllowedToolsResolver;
import org.digit.ai.orchestrator.ConversationOrchestrator;
import org.digit.ai.orchestrator.ToolRegistry;
import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.account.AccountCreateTool;
import org.digit.ai.tools.account.AccountConfigureTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConversationOrchestratorTest {

    @Test
    public void shouldExecuteAllowedTool() {
        ConfigState state = new ConfigState();

        ToolRegistry registry = new ToolRegistry(
            List.of(new AccountCreateTool(), new AccountConfigureTool())
        );

        ConversationOrchestrator orchestrator =
            new ConversationOrchestrator(
                new AllowedToolsResolver(),
                registry
            );

        orchestrator.execute("account.create", state);

        assertThat(state.getAccount().isCreated()).isTrue();
    }

    @Test
    public void shouldRejectDisallowedTool() {
        ConfigState state = new ConfigState();

        ToolRegistry registry = new ToolRegistry(
            List.of(new AccountCreateTool(), new AccountConfigureTool())
        );

        ConversationOrchestrator orchestrator =
            new ConversationOrchestrator(
                new AllowedToolsResolver(),
                registry
            );

        assertThatThrownBy(() ->
            orchestrator.execute("account.configure", state)
        ).isInstanceOf(IllegalStateException.class);
    }
}
