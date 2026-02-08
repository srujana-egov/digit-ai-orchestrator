package org.digit.ai.gating;

import org.digit.ai.state.ConfigState;

import java.util.ArrayList;
import java.util.List;

public class AllowedToolsResolver {

    public List<String> resolve(ConfigState state) {

        // HARD GATE 1: Account must exist
        if (!state.getAccount().isCreated()) {
            return List.of("account.create");
        }

        // HARD GATE 2: Account must be configured (auth token)
        if (!state.getAccount().isConfigured()) {
            return List.of("account.configure");
        }

        // From here on, platform is usable
        List<String> tools = new ArrayList<>();

        // Independent configuration domains
        if (!state.isIdGenConfigured()) {
            tools.add("idgen.configure");
        }

        if (!state.isWorkflowConfigured()) {
            tools.add("workflow.configure");
        }

        if (!state.isNotificationConfigured()) {
            tools.add("notification.configure");
        }

        if (!state.isBoundaryConfigured()) {
            tools.add("boundary.configure");
        }

        if (!state.isRegistrySchemaConfigured()) {
            tools.add("registry.configure");
        }

        // User & Role creation (independent)
        if (!state.getUser().isCreated()) {
            tools.add("user.create");
        }

        if (!state.getRole().isCreated()) {
            tools.add("role.create");
        }

        // Derived capability: role assignment
        if (
            state.getUser().isCreated() &&
            state.getRole().isCreated() &&
            !state.isRoleAssignmentDone()
        ) {
            tools.add("role.assign");
        }

        return tools;
    }
}
