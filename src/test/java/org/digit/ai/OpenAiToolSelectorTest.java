package org.digit.ai.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiToolSelectorTest {

    private OpenAiToolSelector selector;

    @BeforeEach
    void setup() {
        // API key not used in explain-first paths
        selector = new OpenAiToolSelector("dummy-key");
    }

    /* -------------------------------------------------
     * ACCOUNT PREREQUISITES
     * ------------------------------------------------- */

    @Test
    void shouldExplainWhenConfiguringIdGenWithoutAccountSetup() {
        AiDecision decision = selector.decide(
                "generate unique id",
                List.of("account.create")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);

        assertThat(decision.message())
                .contains("create your account");
    }

    @Test
    void shouldExplainWhenConfiguringWorkflowWithoutAccountConfigured() {
        AiDecision decision = selector.decide(
                "configure workflow",
                List.of("account.configure")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    /* -------------------------------------------------
     * USER & ROLE CREATION
     * ------------------------------------------------- */

    @Test
    void shouldExplainWhenCreatingUserBeforeAccountSetup() {
        AiDecision decision = selector.decide(
                "create user",
                List.of("account.create")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldExplainWhenCreatingRoleBeforeAccountSetup() {
        AiDecision decision = selector.decide(
                "create role",
                List.of("account.configure")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    /* -------------------------------------------------
     * ROLE ASSIGNMENT
     * ------------------------------------------------- */

    @Test
    void shouldExplainWhenAssigningRoleWithoutUserOrRole() {
        AiDecision decision = selector.decide(
                "assign role",
                List.of("user.create", "role.create")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);

        assertThat(decision.message())
                .contains("both a user and a role");
    }

    /* -------------------------------------------------
     * HAPPY PATHS
     * ------------------------------------------------- */

    @Test
    void shouldExplainBeforeExecutingIdGenWhenAccountIsReady() {
        AiDecision decision = selector.decide(
                "generate unique id",
                List.of("idgen.configure", "workflow.configure")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);
        
        assertThat(decision.message())
                .contains("idgen.configure");
    }

    @Test
    void shouldExplainBeforeExecutingRoleAssignment() {
        AiDecision decision = selector.decide(
                "assign role",
                List.of("role.assign")
        );

        assertThat(decision.type())
                .isEqualTo(AiDecision.DecisionType.EXPLAIN);

        assertThat(decision.message())
                .contains("role.assign");
    }
}
