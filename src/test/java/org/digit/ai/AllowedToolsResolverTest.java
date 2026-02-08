package org.digit.ai;

import org.digit.ai.gating.AllowedToolsResolver;
import org.digit.ai.state.ConfigState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AllowedToolsResolverTest {

    private final AllowedToolsResolver resolver = new AllowedToolsResolver();

    @Test
    public void shouldOnlyAllowAccountCreationInitially() {
        ConfigState state = new ConfigState();

        List<String> tools = resolver.resolve(state);

        assertThat(tools).containsExactly("account.create");
    }

    @Test
    public void shouldOnlyAllowAccountConfigurationAfterCreation() {
        ConfigState state = new ConfigState();
        state.getAccount().setCreated(true);

        List<String> tools = resolver.resolve(state);

        assertThat(tools).containsExactly("account.configure");
    }

    @Test
    public void shouldAllowIndependentConfigurationsAfterAccountConfigured() {
        ConfigState state = new ConfigState();
        state.getAccount().setCreated(true);
        state.getAccount().setConfigured(true);

        List<String> tools = resolver.resolve(state);

        assertThat(tools).containsExactlyInAnyOrder(
            "idgen.configure",
            "workflow.configure",
            "notification.configure",
            "boundary.configure",
            "registry.configure",
            "user.create",
            "role.create"
        );
    }

    @Test
    public void shouldAllowRoleAssignmentOnlyAfterUserAndRoleCreated() {
        ConfigState state = new ConfigState();
        state.getAccount().setCreated(true);
        state.getAccount().setConfigured(true);
        state.getUser().setCreated(true);
        state.getRole().setCreated(true);

        List<String> tools = resolver.resolve(state);

        assertThat(tools).contains("role.assign");
    }

    @Test
    public void shouldNotAllowRoleAssignmentIfAlreadyDone() {
        ConfigState state = new ConfigState();
        state.getAccount().setCreated(true);
        state.getAccount().setConfigured(true);
        state.getUser().setCreated(true);
        state.getRole().setCreated(true);
        state.setRoleAssignmentDone(true);

        List<String> tools = resolver.resolve(state);

        assertThat(tools).doesNotContain("role.assign");
    }
}
