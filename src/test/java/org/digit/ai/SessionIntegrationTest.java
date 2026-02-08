package org.digit.ai;

import org.digit.ai.ai.AiDecision;
import org.digit.ai.ai.OpenAiToolSelector;
import org.digit.ai.gating.AllowedToolsResolver;
import org.digit.ai.orchestrator.ConversationOrchestrator;
import org.digit.ai.orchestrator.ToolRegistry;
import org.digit.ai.session.ConversationSession;
import org.digit.ai.session.SessionStore;
import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for session handling + intent inference.
 * Tests the complete flow: user message → intent → session → action → state update
 */
class SessionIntegrationTest {

    private SessionStore sessionStore;
    private OpenAiToolSelector selector;
    private AllowedToolsResolver resolver;
    private ConversationOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        sessionStore = new SessionStore();
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "dummy-key-for-offline-testing";
        }
        selector = new OpenAiToolSelector(apiKey);
        
        resolver = new AllowedToolsResolver();
        
        // Create mock tool handlers
        List<ToolHandler> handlers = List.of(
            createMockHandler("account.create"),
            createMockHandler("account.configure"),
            createMockHandler("idgen.configure"),
            createMockHandler("workflow.configure"),
            createMockHandler("boundary.configure"),
            createMockHandler("notification.configure"),
            createMockHandler("registry.configure"),
            createMockHandler("user.create"),
            createMockHandler("role.create"),
            createMockHandler("role.assign")
        );
        
        ToolRegistry registry = new ToolRegistry(handlers);
        orchestrator = new ConversationOrchestrator(resolver, registry);
    }

    private ToolHandler createMockHandler(String name) {
        return new ToolHandler() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void execute(ConfigState state) {
                // Mock execution - just mark as done based on tool name
                switch (name) {
                    case "account.create" -> state.getAccount().setCreated(true);
                    case "account.configure" -> state.getAccount().setConfigured(true);
                    case "idgen.configure" -> state.setIdGenConfigured(true);
                    case "workflow.configure" -> state.setWorkflowConfigured(true);
                    case "boundary.configure" -> state.setBoundaryConfigured(true);
                    case "notification.configure" -> state.setNotificationConfigured(true);
                    case "registry.configure" -> state.setRegistrySchemaConfigured(true);
                    case "user.create" -> state.getUser().setCreated(true);
                    case "role.create" -> state.getRole().setCreated(true);
                    case "role.assign" -> state.setRoleAssignmentDone(true);
                }
            }
        };
    }

    /* ========================================
     * BASIC SESSION FLOW
     * ======================================== */

    @Test
    void shouldRememberPendingActionAcrossCalls() {
        ConversationSession session = sessionStore.getSession("test-1");
        
        // First call: AI proposes action
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("configure workflow", allowedTools);
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.proposedAction()).isNotNull();
        
        // Store pending action
        session.setPendingAction(decision.proposedAction());
        
        // Verify it's stored
        assertThat(session.getPendingAction()).isEqualTo("account.create");
    }

    @Test
    void shouldExecutePendingActionOnYes() {
        ConversationSession session = sessionStore.getSession("test-2");
        
        // Setup: store a pending action
        session.setPendingAction("account.create");
        
        // Simulate YES response
        String message = "yes";
        
        if (message.equals("yes") && session.getPendingAction() != null) {
            String action = session.getPendingAction();
            session.clearPendingAction();
            
            orchestrator.execute(action, session.getState());
            
            assertThat(session.getPendingAction()).isNull();
            assertThat(session.getState().getAccount().isCreated()).isTrue();
        }
    }

    @Test
    void shouldClearPendingActionOnNo() {
        ConversationSession session = sessionStore.getSession("test-3");
        
        // Setup: store a pending action
        session.setPendingAction("account.create");
        
        // Simulate NO response
        String message = "no";
        
        if (message.equals("no") && session.getPendingAction() != null) {
            session.clearPendingAction();
            
            assertThat(session.getPendingAction()).isNull();
            assertThat(session.getState().getAccount().isCreated()).isFalse();
        }
    }

    /* ========================================
     * COMPLETE CONVERSATION FLOWS
     * ======================================== */

    @Test
    void shouldCompleteBootstrapFlow() {
        ConversationSession session = sessionStore.getSession("bootstrap-flow");
        
        // Step 1: User asks to get started
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("how do i start", allowedTools);
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        session.setPendingAction(decision.proposedAction());
        
        // Step 2: User says yes
        String action = session.getPendingAction();
        session.clearPendingAction();
        orchestrator.execute(action, session.getState());
        
        assertThat(session.getState().getAccount().isCreated()).isTrue();
        
        // Step 3: Next action should be account.configure
        allowedTools = orchestrator.getAllowedTools(session.getState());
        assertThat(allowedTools).containsExactly("account.configure");
    }

    @Test
    void shouldCompleteIdgenConfigurationFlow() {
        ConversationSession session = sessionStore.getSession("idgen-flow");
        
        // Step 1: User asks about unique codes (account not ready)
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("i need unique codes", allowedTools);
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        session.setPendingAction(decision.proposedAction());
        
        // Step 2: User confirms
        orchestrator.execute(session.getPendingAction(), session.getState());
        session.clearPendingAction();
        
        // Step 3: Account configure
        allowedTools = orchestrator.getAllowedTools(session.getState());
        orchestrator.execute("account.configure", session.getState());
        
        // Step 4: Now idgen should be available
        allowedTools = orchestrator.getAllowedTools(session.getState());
        assertThat(allowedTools).contains("idgen.configure");
        
        // Step 5: User asks again about unique codes
        decision = selector.decide("i need unique codes", allowedTools);
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.proposedAction()).isEqualTo("idgen.configure");
    }

    @Test
    void shouldHandleUserDecliningAndChangingMind() {
        ConversationSession session = sessionStore.getSession("decline-flow");
        
        // Step 1: User asks about workflow
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("setup workflow", allowedTools);
        
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        session.setPendingAction(decision.proposedAction());
        
        // Step 2: User declines
        session.clearPendingAction();
        assertThat(session.getPendingAction()).isNull();
        
        // Step 3: User asks about something else
        decision = selector.decide("i need unique ids", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        session.setPendingAction(decision.proposedAction());
        
        // Step 4: User confirms this time
        orchestrator.execute(session.getPendingAction(), session.getState());
        session.clearPendingAction();
        
        assertThat(session.getState().getAccount().isCreated()).isTrue();
    }

    /* ========================================
     * SESSION ISOLATION
     * ======================================== */

    @Test
    void shouldIsolateDifferentSessions() {
        ConversationSession session1 = sessionStore.getSession("user-1");
        ConversationSession session2 = sessionStore.getSession("user-2");
        
        // Session 1: User asks about workflow
        var allowedTools1 = orchestrator.getAllowedTools(session1.getState());
        AiDecision decision1 = selector.decide("configure workflow", allowedTools1);
        session1.setPendingAction(decision1.proposedAction());
        
        // Session 2: User asks about idgen
        var allowedTools2 = orchestrator.getAllowedTools(session2.getState());
        AiDecision decision2 = selector.decide("unique codes", allowedTools2);
        session2.setPendingAction(decision2.proposedAction());
        
        // Both should have account.create pending
        assertThat(session1.getPendingAction()).isEqualTo("account.create");
        assertThat(session2.getPendingAction()).isEqualTo("account.create");
        
        // Session 1 confirms
        orchestrator.execute(session1.getPendingAction(), session1.getState());
        session1.clearPendingAction();
        
        // Session 1 should be updated, session 2 unchanged
        assertThat(session1.getState().getAccount().isCreated()).isTrue();
        assertThat(session2.getState().getAccount().isCreated()).isFalse();
        assertThat(session2.getPendingAction()).isEqualTo("account.create");
    }

    @Test
    void shouldMaintainSeparateStatePerSession() {
        ConversationSession session1 = sessionStore.getSession("state-1");
        ConversationSession session2 = sessionStore.getSession("state-2");
        
        // Session 1: Complete account setup
        orchestrator.execute("account.create", session1.getState());
        orchestrator.execute("account.configure", session1.getState());
        
        // Session 2: Still at beginning
        var allowedTools1 = orchestrator.getAllowedTools(session1.getState());
        var allowedTools2 = orchestrator.getAllowedTools(session2.getState());
        
        assertThat(allowedTools1).doesNotContain("account.create", "account.configure");
        assertThat(allowedTools2).containsExactly("account.create");
    }

    /* ========================================
     * INTENT + SESSION COMBINATIONS
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'configure workflow', workflow",
        "'i need unique codes', idgen",
        "'setup notifications', notification",
        "'add data', registry",
        "'create user', user",
        "'create role', role"
    })
    void shouldInferIntentAndProposeAccountSetup(String userMessage, String expectedIntent) {
        ConversationSession session = sessionStore.getSession("intent-" + expectedIntent);
        
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide(userMessage, allowedTools);
        
        // Should explain need for account setup
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        assertThat(decision.message().toLowerCase()).contains("account");
    }

    @Test
    void shouldProposeCorrectToolAfterAccountSetup() {
        ConversationSession session = sessionStore.getSession("after-setup");
        
        // Complete account setup
        orchestrator.execute("account.create", session.getState());
        orchestrator.execute("account.configure", session.getState());
        
        // Now ask about workflow
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("configure workflow", allowedTools);
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.proposedAction()).isEqualTo("workflow.configure");
    }

    /* ========================================
     * EDGE CASES
     * ======================================== */

    @Test
    void shouldHandleYesWithoutPendingAction() {
        ConversationSession session = sessionStore.getSession("no-pending");
        
        // No pending action
        assertThat(session.getPendingAction()).isNull();
        
        // User says yes anyway
        String message = "yes";
        
        if (message.equals("yes") && session.getPendingAction() != null) {
            // Should not execute
            assertThat(true).isFalse(); // This should not be reached
        }
        
        // Should handle gracefully - need to ask AI what to do
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide(message, allowedTools);
        
        // AI should handle "yes" as unknown intent
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleMultipleYesInRow() {
        ConversationSession session = sessionStore.getSession("multi-yes");
        
        // Setup pending action
        session.setPendingAction("account.create");
        
        // First yes
        orchestrator.execute(session.getPendingAction(), session.getState());
        session.clearPendingAction();
        
        assertThat(session.getState().getAccount().isCreated()).isTrue();
        
        // Second yes (no pending action)
        assertThat(session.getPendingAction()).isNull();
        
        // Should not crash, just treat as regular message
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("yes", allowedTools);
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldOverwritePendingActionIfUserChangesIntent() {
        ConversationSession session = sessionStore.getSession("change-intent");
        
        // User asks about workflow
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision1 = selector.decide("configure workflow", allowedTools);
        session.setPendingAction(decision1.proposedAction());
        
        assertThat(session.getPendingAction()).isEqualTo("account.create");
        
        // Before confirming, user asks about something else
        AiDecision decision2 = selector.decide("i need unique codes", allowedTools);
        session.setPendingAction(decision2.proposedAction());
        
        // Should still be account.create (same prerequisite)
        assertThat(session.getPendingAction()).isEqualTo("account.create");
        
        // But if account was ready and user changed intent
        orchestrator.execute("account.create", session.getState());
        orchestrator.execute("account.configure", session.getState());
        
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision1 = selector.decide("configure workflow", allowedTools);
        session.setPendingAction(decision1.proposedAction());
        assertThat(session.getPendingAction()).isEqualTo("workflow.configure");
        
        // User changes mind to idgen
        decision2 = selector.decide("i need unique codes", allowedTools);
        session.setPendingAction(decision2.proposedAction());
        assertThat(session.getPendingAction()).isEqualTo("idgen.configure");
    }

    @Test
    void shouldHandleCaseSensitiveYesNo() {
        ConversationSession session = sessionStore.getSession("case-test");
        
        session.setPendingAction("account.create");
        
        // Test various cases
        String[] yesVariations = {"yes", "YES", "Yes", "yEs"};
        
        for (String yes : yesVariations) {
            String normalized = yes.toLowerCase().trim();
            assertThat(normalized).isEqualTo("yes");
        }
        
        String[] noVariations = {"no", "NO", "No", "nO"};
        
        for (String no : noVariations) {
            String normalized = no.toLowerCase().trim();
            assertThat(normalized).isEqualTo("no");
        }
    }

    /* ========================================
     * FULL END-TO-END SCENARIOS
     * ======================================== */

    @Test
    void shouldCompleteFullOnboardingJourney() {
        ConversationSession session = sessionStore.getSession("full-journey");
        
        // 1. User starts
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("how do i start", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        
        // 2. User confirms
        orchestrator.execute(decision.proposedAction(), session.getState());
        
        // 3. Account configure
        allowedTools = orchestrator.getAllowedTools(session.getState());
        orchestrator.execute("account.configure", session.getState());
        
        // 4. User wants workflow
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision = selector.decide("configure workflow", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("workflow.configure");
        orchestrator.execute(decision.proposedAction(), session.getState());
        
        // 5. User wants idgen
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision = selector.decide("unique codes", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("idgen.configure");
        orchestrator.execute(decision.proposedAction(), session.getState());
        
        // 6. User wants to create user
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision = selector.decide("create user", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("user.create");
        orchestrator.execute(decision.proposedAction(), session.getState());
        
        // 7. User wants to create role
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision = selector.decide("create role", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("role.create");
        orchestrator.execute(decision.proposedAction(), session.getState());
        
        // 8. User wants to assign role
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision = selector.decide("assign role", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("role.assign");
        
        // Verify final state
        assertThat(session.getState().getAccount().isCreated()).isTrue();
        assertThat(session.getState().getAccount().isConfigured()).isTrue();
        assertThat(session.getState().isWorkflowConfigured()).isTrue();
        assertThat(session.getState().isIdGenConfigured()).isTrue();
        assertThat(session.getState().getUser().isCreated()).isTrue();
        assertThat(session.getState().getRole().isCreated()).isTrue();
    }

    @Test
    void shouldHandleNonLinearJourney() {
        ConversationSession session = sessionStore.getSession("non-linear");
        
        // User jumps around asking about different things
        
        // 1. Asks about registry
        var allowedTools = orchestrator.getAllowedTools(session.getState());
        AiDecision decision = selector.decide("how to add data", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        
        // 2. Declines
        session.clearPendingAction();
        
        // 3. Asks about notifications
        decision = selector.decide("setup notifications", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        
        // 4. Declines again
        session.clearPendingAction();
        
        // 5. Finally asks to get started
        decision = selector.decide("ok let's start", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("account.create");
        
        // 6. Confirms
        orchestrator.execute(decision.proposedAction(), session.getState());
        orchestrator.execute("account.configure", session.getState());
        
        // 7. Now can do registry
        allowedTools = orchestrator.getAllowedTools(session.getState());
        decision = selector.decide("how to add data", allowedTools);
        assertThat(decision.proposedAction()).isEqualTo("registry.configure");
    }
}
