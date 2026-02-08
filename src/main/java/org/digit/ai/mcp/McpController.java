package org.digit.ai.mcp;

import org.digit.ai.ai.AiToolSelector;
import org.digit.ai.ai.AiDecision;
import org.digit.ai.orchestrator.ConversationOrchestrator;
import org.digit.ai.session.ConversationSession;
import org.digit.ai.session.SessionStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final ConversationOrchestrator orchestrator;
    private final AiToolSelector aiToolSelector;
    private final SessionStore sessionStore = new SessionStore();

    public McpController(
            ConversationOrchestrator orchestrator,
            AiToolSelector aiToolSelector
    ) {
        this.orchestrator = orchestrator;
        this.aiToolSelector = aiToolSelector;
    }

    @GetMapping("/allowed-tools")
    public AllowedToolsResponse allowedTools(
            @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId
    ) {
        ConversationSession session = sessionStore.getSession(sessionId);
        return new AllowedToolsResponse(
                orchestrator.getAllowedTools(session.getState())
        );
    }

    @PostMapping("/ai")
    public ToolExecuteResponse aiExecute(
            @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId,
            @RequestBody AiRequest request
    ) {
        try {
            ConversationSession session = sessionStore.getSession(sessionId);
            String message = request.message().toLowerCase().trim();

            // YES handling
            if (message.equals("yes") && session.getPendingAction() != null) {
                String action = session.getPendingAction();
                session.clearPendingAction();

                orchestrator.execute(action, session.getState());

                return new ToolExecuteResponse(true, "Executed: " + action);
            }

            // NO handling
            if (message.equals("no") && session.getPendingAction() != null) {
                session.clearPendingAction();
                return new ToolExecuteResponse(true, "Okay, let me know what you'd like to do next.");
            }

            // Get AI decision
            var allowedTools = orchestrator.getAllowedTools(session.getState());

            AiDecision decision =
                    aiToolSelector.decide(
                            request.message(),
                            allowedTools
                    );

            if (decision.type() == AiDecision.DecisionType.EXPLAIN) {
                // Store proposed action if present
                if (decision.proposedAction() != null) {
                    session.setPendingAction(decision.proposedAction());
                }
                return new ToolExecuteResponse(false, decision.message());
            }

            orchestrator.execute(decision.tool(), session.getState());

            return new ToolExecuteResponse(
                    true,
                    "Executed: " + decision.tool()
            );

        } catch (Exception e) {
            return new ToolExecuteResponse(false, e.getMessage());
        }
    }
}
