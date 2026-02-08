package org.digit.ai.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive intent inference testing.
 * Tests cover naive users, ambiguous queries, and edge cases.
 * Goal: 100% accuracy in intent classification.
 */
class IntentInferenceTest {

    private OpenAiToolSelector selector;

    @BeforeEach
    void setup() {
        // Use real API key from environment or dummy for offline testing
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "dummy-key-for-offline-testing";
        }
        selector = new OpenAiToolSelector(apiKey);
    }

    /* ========================================
     * BOOTSTRAP / GETTING STARTED
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'how do i start', bootstrap",
        "'getting started', bootstrap",
        "'i'm new here', bootstrap",
        "'what do i do first', bootstrap",
        "'help me set up', bootstrap",
        "'initial setup', bootstrap",
        "'first time user', bootstrap",
        "'i don't know what to do', bootstrap",
        "'where do i begin', bootstrap",
        "'setup everything', bootstrap"
    })
    void shouldInferBootstrapIntent(String userMessage, String expectedIntent) {
        AiDecision decision = selector.decide(userMessage, List.of("account.create"));
        
        // Should ask to create account for bootstrap
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("account");
    }

    /* ========================================
     * ACCOUNT CONFIGURATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'how do i set up account details'",
        "'configure my account'",
        "'i need to add account information'",
        "'setup account after creation'",
        "'authenticate my account'",
        "'account configuration'",
        "'how to configure account'",
        "'set account details'",
        "'i created account now what'",
        "'account setup details'"
    })
    void shouldInferAccountConfigureIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("account.configure"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("account.configure");
    }

    /* ========================================
     * IDGEN - UNIQUE ID GENERATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'i need unique codes'",
        "'how to generate ids'",
        "'create unique identifiers'",
        "'auto increment numbers'",
        "'sequence generation'",
        "'i want unique id for each record'",
        "'how do i get unique codes'",
        "'generate application numbers'",
        "'auto generate reference numbers'",
        "'unique identifier setup'",
        "'i need some kind of unique code'",
        "'how to create unique ids'",
        "'id generation'",
        "'sequence numbers'"
    })
    void shouldInferIdgenIntent(String userMessage) {
        // Account already set up, idgen available
        AiDecision decision = selector.decide(userMessage, List.of("idgen.configure"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldNotConfuseIdgenWithOtherIntents() {
        // "valid" contains "id" but shouldn't infer idgen intent
        AiDecision decision = selector.decide(
            "how to validate data",
            List.of("registry.configure", "idgen.configure")
        );
        
        // Should prefer registry over idgen for data validation
        assertThat(decision.message().toLowerCase())
            .contains("registry");
    }

    /* ========================================
     * WORKFLOW CONFIGURATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'setup workflow'",
        "'configure business process'",
        "'i need approval flow'",
        "'how to create workflow'",
        "'state machine setup'",
        "'process configuration'",
        "'workflow needs to be set up'",
        "'i don't know what i need but workflow'",
        "'approval process'",
        "'business flow setup'",
        "'how do i configure workflows'",
        "'transition setup'",
        "'state management'"
    })
    void shouldInferWorkflowIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("workflow.configure"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    /* ========================================
     * REGISTRY - DATA SCHEMAS
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'how to add data'",
        "'i need to add data'",
        "'setup data schema'",
        "'configure data model'",
        "'how do i define entities'",
        "'registry setup'",
        "'data structure configuration'",
        "'i want to add registry data'",
        "'how to manage data'",
        "'schema configuration'",
        "'entity definition'",
        "'data model setup'",
        "'how to add registry schemas'"
    })
    void shouldInferRegistryIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("registry.configure"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("registry");
    }

    /* ========================================
     * BOUNDARY CONFIGURATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'setup boundaries'",
        "'configure geographic hierarchy'",
        "'i need location setup'",
        "'administrative boundaries'",
        "'how to add boundaries'",
        "'boundary configuration'",
        "'location hierarchy'",
        "'geographic setup'",
        "'area configuration'"
    })
    void shouldInferBoundaryIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("boundary.configure"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("boundary");
    }

    /* ========================================
     * NOTIFICATION CONFIGURATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'setup notifications'",
        "'configure email alerts'",
        "'i need sms notifications'",
        "'how to send notifications'",
        "'notification templates'",
        "'alert configuration'",
        "'email setup'",
        "'sms configuration'",
        "'communication setup'"
    })
    void shouldInferNotificationIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("notification.configure"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("notification");
    }

    /* ========================================
     * USER CREATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'create user'",
        "'add new user'",
        "'i need to create users'",
        "'user creation'",
        "'how to add users'",
        "'setup user accounts'",
        "'add user to system'",
        "'user management'",
        "'create user account'"
    })
    void shouldInferUserIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("user.create"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("user");
    }

    @Test
    void shouldNotConfuseUserWithAccountConfigure() {
        // "user" intent should not trigger account.configure
        AiDecision decision = selector.decide(
            "create user",
            List.of("user.create", "account.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("user")
            .doesNotContain("account.configure");
    }

    /* ========================================
     * ROLE CREATION
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'create role'",
        "'add new role'",
        "'i need to create roles'",
        "'role creation'",
        "'how to add roles'",
        "'setup roles'",
        "'permission groups'",
        "'access control setup'",
        "'define roles'"
    })
    void shouldInferRoleIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("role.create"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("role");
    }

    /* ========================================
     * ROLE ASSIGNMENT
     * ======================================== */

    @ParameterizedTest
    @CsvSource({
        "'assign role to user'",
        "'give user a role'",
        "'i need to assign roles'",
        "'role assignment'",
        "'grant permissions'",
        "'assign permissions to user'",
        "'how to assign roles'",
        "'user role mapping'"
    })
    void shouldInferRoleAssignIntent(String userMessage) {
        AiDecision decision = selector.decide(userMessage, List.of("role.assign"));
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("role.assign");
    }

    /* ========================================
     * AMBIGUOUS / EDGE CASES
     * ======================================== */

    @Test
    void shouldHandleVagueQuery() {
        AiDecision decision = selector.decide(
            "i need help",
            List.of("account.create")
        );
        
        // Should default to bootstrap/account creation
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleEmptyQuery() {
        AiDecision decision = selector.decide(
            "",
            List.of("account.create")
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleMultipleIntentsInOneQuery() {
        // User mentions multiple things - should pick the most relevant
        AiDecision decision = selector.decide(
            "i need to setup workflow and also create users",
            List.of("workflow.configure", "user.create")
        );
        
        // Should pick one (preferably workflow as it's mentioned first)
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .containsAnyOf("workflow", "user");
    }

    @Test
    void shouldHandleTypos() {
        AiDecision decision = selector.decide(
            "i ned to crete uniqe ids",  // typos: ned, crete, uniqe
            List.of("idgen.configure")
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleCasualLanguage() {
        AiDecision decision = selector.decide(
            "yo how do i get some unique codes going",
            List.of("idgen.configure")
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleVerboseQuery() {
        AiDecision decision = selector.decide(
            "Hello, I am new to this system and I was wondering if you could help me " +
            "understand how I can configure the system to generate unique identification " +
            "codes for each application that gets submitted in our workflow",
            List.of("idgen.configure", "workflow.configure")
        );
        
        // Should recognize idgen as primary intent
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    /* ========================================
     * PREREQUISITE CHECKING
     * ======================================== */

    @Test
    void shouldExplainPrerequisitesForIdgen() {
        AiDecision decision = selector.decide(
            "i need unique codes",
            List.of("account.create")  // Account not ready
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("account");
    }

    @Test
    void shouldExplainPrerequisitesForWorkflow() {
        AiDecision decision = selector.decide(
            "setup workflow",
            List.of("account.configure")  // Account not configured
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
        assertThat(decision.message().toLowerCase())
            .contains("account");
    }

    /* ========================================
     * FALLBACK BEHAVIOR
     * ======================================== */

    @Test
    void shouldFallbackGracefullyOnUnknownIntent() {
        AiDecision decision = selector.decide(
            "what's the weather like",
            List.of("account.create")
        );
        
        // Should still provide some guidance
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    /* ========================================
     * ADVERSARIAL / TRICKY CASES
     * ======================================== */

    @Test
    void shouldHandleWordContainingId() {
        // Words containing "id": valid, provide, guide, video, idea, identity
        AiDecision decision = selector.decide(
            "provide guidance on validation",
            List.of("registry.configure", "idgen.configure")
        );
        
        // Should NOT infer idgen
        assertThat(decision.message().toLowerCase())
            .contains("registry");
    }

    @Test
    void shouldDistinguishAccountFromUser() {
        // "account" should map to account.configure, not user.create
        AiDecision decision = selector.decide(
            "i need to setup my account",
            List.of("account.configure", "user.create")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("account")
            .doesNotContain("user.create");
    }

    @Test
    void shouldDistinguishUserFromAccount() {
        // "user" should map to user.create, not account.configure
        AiDecision decision = selector.decide(
            "add a new user to the system",
            List.of("account.configure", "user.create")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("user");
    }

    @Test
    void shouldHandleDataAmbiguity() {
        // "data" could mean registry or idgen, but registry is more likely
        AiDecision decision = selector.decide(
            "i need to manage data",
            List.of("registry.configure", "idgen.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("registry");
    }

    @Test
    void shouldHandleCodeAmbiguity() {
        // "code" could mean idgen or programming
        AiDecision decision = selector.decide(
            "i need unique codes for applications",
            List.of("idgen.configure", "workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleFlowAmbiguity() {
        // "flow" should map to workflow
        AiDecision decision = selector.decide(
            "setup approval flow",
            List.of("workflow.configure", "idgen.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandleSchemaKeyword() {
        // "schema" should map to registry
        AiDecision decision = selector.decide(
            "define schema",
            List.of("registry.configure", "workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("registry");
    }

    @Test
    void shouldHandleLocationKeywords() {
        // Location-related should map to boundary
        AiDecision decision = selector.decide(
            "setup locations and areas",
            List.of("boundary.configure", "registry.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("boundary");
    }

    @Test
    void shouldHandleAlertKeywords() {
        // Alert/notification keywords
        AiDecision decision = selector.decide(
            "send alerts to users",
            List.of("notification.configure", "user.create")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("notification");
    }

    @Test
    void shouldHandlePermissionKeywords() {
        // Permission could be role or role.assign
        AiDecision decision = selector.decide(
            "setup permissions",
            List.of("role.create", "role.assign")
        );
        
        // Should prefer role.create for "setup"
        assertThat(decision.message().toLowerCase())
            .containsAnyOf("role.create", "role");
    }

    @Test
    void shouldHandleGrantKeyword() {
        // "grant" should map to role.assign
        AiDecision decision = selector.decide(
            "grant access to user",
            List.of("role.create", "role.assign")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("role.assign");
    }

    @Test
    void shouldHandleNonEnglishLikeInput() {
        // Gibberish or non-English
        AiDecision decision = selector.decide(
            "asdfghjkl",
            List.of("account.create")
        );
        
        // Should fallback gracefully
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleVeryLongQuery() {
        String longQuery = "I am a new user and I have been trying to understand how this system works. " +
            "I read through some documentation but I'm still confused. What I really need is to be able to " +
            "generate unique identification numbers for each application that comes into our system. " +
            "These numbers need to be sequential and unique across the entire platform. Can you help me " +
            "set this up? I'm not sure where to start or what configuration is needed. Please guide me " +
            "through the process step by step if possible.";
        
        AiDecision decision = selector.decide(longQuery, List.of("idgen.configure"));
        
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleQuestionFormat() {
        AiDecision decision = selector.decide(
            "can you help me configure workflows?",
            List.of("workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandleImperativeFormat() {
        AiDecision decision = selector.decide(
            "configure the workflow system",
            List.of("workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandlePassiveFormat() {
        AiDecision decision = selector.decide(
            "workflow needs to be configured",
            List.of("workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandleMixedCase() {
        AiDecision decision = selector.decide(
            "I NEED TO SETUP IDGEN",
            List.of("idgen.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleSpecialCharacters() {
        AiDecision decision = selector.decide(
            "setup workflow!!!",
            List.of("workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandleNumbersInQuery() {
        AiDecision decision = selector.decide(
            "i need 100 unique ids for my application",
            List.of("idgen.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleAcronyms() {
        AiDecision decision = selector.decide(
            "setup SMS notifications",
            List.of("notification.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("notification");
    }

    @Test
    void shouldHandleNegation() {
        // "don't know" should still infer intent from context
        AiDecision decision = selector.decide(
            "i don't know how to setup workflows",
            List.of("workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandleComparison() {
        AiDecision decision = selector.decide(
            "what's the difference between user and role",
            List.of("user.create", "role.create")
        );
        
        // Should pick one or explain both
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleSequenceRequest() {
        AiDecision decision = selector.decide(
            "i need sequential numbers",
            List.of("idgen.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleAutoIncrementRequest() {
        AiDecision decision = selector.decide(
            "auto increment ids",
            List.of("idgen.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("idgen");
    }

    @Test
    void shouldHandleEntityMention() {
        AiDecision decision = selector.decide(
            "define entities and attributes",
            List.of("registry.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("registry");
    }

    @Test
    void shouldHandleStateMachineMention() {
        AiDecision decision = selector.decide(
            "state machine configuration",
            List.of("workflow.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("workflow");
    }

    @Test
    void shouldHandleHierarchyMention() {
        AiDecision decision = selector.decide(
            "geographic hierarchy setup",
            List.of("boundary.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("boundary");
    }

    @Test
    void shouldHandleTemplateMention() {
        AiDecision decision = selector.decide(
            "email templates",
            List.of("notification.configure")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("notification");
    }

    @Test
    void shouldHandleAccessControlMention() {
        AiDecision decision = selector.decide(
            "access control setup",
            List.of("role.create")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("role");
    }

    @Test
    void shouldHandleAuthenticationMention() {
        AiDecision decision = selector.decide(
            "authentication setup",
            List.of("account.configure", "user.create")
        );
        
        assertThat(decision.message().toLowerCase())
            .contains("account");
    }

    @Test
    void shouldHandleOnboardingMention() {
        AiDecision decision = selector.decide(
            "onboarding process",
            List.of("account.create", "workflow.configure")
        );
        
        // Could be either, but should pick one
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleConfigurationMention() {
        // Generic "configuration" with no context
        AiDecision decision = selector.decide(
            "configuration",
            List.of("account.create")
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }

    @Test
    void shouldHandleSetupMention() {
        // Generic "setup" with no context
        AiDecision decision = selector.decide(
            "setup",
            List.of("account.create")
        );
        
        assertThat(decision.type()).isEqualTo(AiDecision.DecisionType.EXPLAIN);
    }
}
