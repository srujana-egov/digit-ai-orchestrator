package org.digit.ai.state;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConfigState {

    private AccountState account = new AccountState();

    private boolean idGenConfigured;
    private boolean workflowConfigured;
    private boolean notificationConfigured;
    private boolean boundaryConfigured;
    private boolean registrySchemaConfigured;

    private UserState user = new UserState();
    private RoleState role = new RoleState();
    private boolean roleAssignmentDone;
}
