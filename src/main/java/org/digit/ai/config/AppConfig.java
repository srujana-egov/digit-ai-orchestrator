package org.digit.ai.config;

import org.digit.ai.gating.AllowedToolsResolver;
import org.digit.ai.orchestrator.ConversationOrchestrator;
import org.digit.ai.orchestrator.ToolRegistry;

// account tools
import org.digit.ai.tools.account.AccountCreateTool;
import org.digit.ai.tools.account.AccountConfigureTool;

// user / role tools
import org.digit.ai.tools.user.UserCreateTool;
import org.digit.ai.tools.role.RoleCreateTool;
import org.digit.ai.tools.role.RoleAssignTool;

// configuration tools
import org.digit.ai.tools.idgen.IdGenConfigureTool;
import org.digit.ai.tools.workflow.WorkflowConfigureTool;
import org.digit.ai.tools.notification.NotificationConfigureTool;
import org.digit.ai.tools.boundary.BoundaryConfigureTool;
import org.digit.ai.tools.registry.RegistryConfigureTool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import org.digit.ai.ai.AiToolSelector;
import org.digit.ai.ai.OpenAiToolSelector;



@Configuration
public class AppConfig {

    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry(
            List.of(
                new AccountCreateTool(),
                new AccountConfigureTool(),

                new UserCreateTool(),
                new RoleCreateTool(),
                new RoleAssignTool(),

                new IdGenConfigureTool(),
                new WorkflowConfigureTool(),
                new NotificationConfigureTool(),
                new BoundaryConfigureTool(),
                new RegistryConfigureTool()
            )
        );
    }

    @Bean
    public ConversationOrchestrator orchestrator(
            ToolRegistry registry
    ) {
        return new ConversationOrchestrator(
            new AllowedToolsResolver(),
            registry
        );
    }

    @Bean
public AiToolSelector aiToolSelector() {
    return new OpenAiToolSelector(
        System.getenv("OPENAI_API_KEY")
    );
}


}
