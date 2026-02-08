package org.digit.ai.ai;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class OpenAiToolSelector implements AiToolSelector {

    private static final String OPENAI_URL =
            "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiToolSelector(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public AiDecision decide(String userMessage, List<String> allowedTools) {

        /* -------------------------------------------------
         * Use AI to infer user intent from their message
         * ------------------------------------------------- */
        String intent;
        try {
            intent = inferIntentWithAi(userMessage);
        } catch (Exception e) {
            // Fallback to simple keyword matching
            intent = inferIntentSimple(userMessage);
        }

        /* -------------------------------------------------
         * BOOTSTRAP: Initial setup
         * ------------------------------------------------- */
        if ("bootstrap".equals(intent)) {
            if (allowedTools.contains("account.create")) {
                return AiDecision.explain(
                    "To get started, I need to create your DIGIT account. " +
                    "Shall I proceed with account.create?",
                    "account.create"
                );
            }
            if (allowedTools.contains("account.configure")) {
                return AiDecision.explain(
                    "To get started, I need to configure your DIGIT account. " +
                    "Shall I proceed with account.configure?",
                    "account.configure"
                );
            }
            return AiDecision.explain(
                "Initial setup is complete. You can now configure workflows, IDs, users, roles, or boundaries."
            );
        }

        /* -------------------------------------------------
         * ACCOUNT CONFIGURATION
         * ------------------------------------------------- */
        if ("account.configure".equals(intent)) {
            if (allowedTools.contains("account.configure")) {
                return AiDecision.explain(
                    "I understand you want to configure your account details. " +
                    "Shall I proceed with account.configure?",
                    "account.configure"
                );
            }
            if (allowedTools.contains("account.create")) {
                return AiDecision.explain(
                    "Before configuring account details, I need to create the account first. " +
                    "Shall I proceed with account.create?",
                    "account.create"
                );
            }
            return AiDecision.explain(
                "Account is already configured. You can now configure workflows, IDs, users, roles, or boundaries."
            );
        }

        /* -------------------------------------------------
         * CONFIGURATION INTENTS: Check account prerequisites
         * ------------------------------------------------- */
        if (isConfigureIntent(intent)) {
            boolean accountReady =
                    !allowedTools.contains("account.create")
                 && !allowedTools.contains("account.configure");

            if (!accountReady) {
                // Determine which account action to propose
                String proposedAction = allowedTools.contains("account.create") 
                    ? "account.create" 
                    : "account.configure";
                
                String actionDescription = allowedTools.contains("account.create")
                    ? "create your account"
                    : "configure your account";
                
                return AiDecision.explain(
                    "Before I can configure "
                    + intent
                    + ", I need to " + actionDescription + " first. "
                    + "Shall I proceed with that?",
                    proposedAction
                );
            }
        }

        /* -------------------------------------------------
         * USER & ROLE CREATION: Check prerequisites
         * ------------------------------------------------- */
        if ("user".equals(intent) && !allowedTools.contains("user.create")) {
            // Determine which account action to propose
            String proposedAction = allowedTools.contains("account.create") 
                ? "account.create" 
                : "account.configure";
            
            return AiDecision.explain(
                "User creation is available after account setup. " +
                "Shall I proceed with the required steps?",
                proposedAction
            );
        }

        if ("role".equals(intent) && !allowedTools.contains("role.create")) {
            // Determine which account action to propose
            String proposedAction = allowedTools.contains("account.create") 
                ? "account.create" 
                : "account.configure";
            
            return AiDecision.explain(
                "Role creation is available after account setup. " +
                "Shall I proceed with the required steps?",
                proposedAction
            );
        }

        /* -------------------------------------------------
         * ROLE ASSIGNMENT: Check prerequisites
         * ------------------------------------------------- */
        if ("role.assign".equals(intent) && !allowedTools.contains("role.assign")) {
            // Determine what's missing - user or role
            String proposedAction = allowedTools.contains("user.create") 
                ? "user.create" 
                : "role.create";
            
            return AiDecision.explain(
                "To assign a role, both a user and a role must exist first. " +
                "Shall I create the missing pieces?",
                proposedAction
            );
        }

        /* -------------------------------------------------
         * Execute the appropriate tool based on intent
         * ------------------------------------------------- */
        String toolToExecute = mapIntentToTool(intent, allowedTools);
        
        if (toolToExecute != null && allowedTools.contains(toolToExecute)) {
            // Always ask before executing
            return AiDecision.explain(
                "I understand you want to " + getIntentDescription(intent) + ". " +
                "Shall I proceed with " + toolToExecute + "?",
                toolToExecute
            );
        }

        // Final fallback: explain what's available
        return AiDecision.explain(
            "I'm not sure what you'd like to do. Available options: " + 
            String.join(", ", allowedTools)
        );
    }

    // -------------------------------------------------
    // Helpers
    // -------------------------------------------------

    private boolean isConfigureIntent(String intent) {
        return intent.equals("idgen")
            || intent.equals("workflow")
            || intent.equals("boundary")
            || intent.equals("notification")
            || intent.equals("registry");
    }

    /**
     * Use AI to infer the user's intent from their message
     */
    private String inferIntentWithAi(String message) throws Exception {
        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
            "temperature", 0,
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content",
                    "You are an intent classifier for a DIGIT platform setup assistant. " +
                    "Analyze the user's message and return EXACTLY ONE of these intents:\n\n" +
                    "- bootstrap: Initial setup, getting started, first time setup, 'how do i start'\n" +
                    "- account.configure: Setting up account details, authentication, account configuration AFTER account creation\n" +
                    "- idgen: Unique ID generation, auto-incrementing codes, sequence numbers, identifiers\n" +
                    "- workflow: Business process configuration, state machines, approval flows, transitions\n" +
                    "- boundary: Geographic hierarchies, administrative boundaries, location setup\n" +
                    "- notification: Email/SMS alerts, notification templates, communication setup\n" +
                    "- registry: Data schemas, data models, entity definitions, adding/managing data structures\n" +
                    "- user: User account creation, user management, adding users (NOT account setup)\n" +
                    "- role: Role creation, permission groups, access control roles\n" +
                    "- role.assign: Assigning roles to users, granting permissions\n" +
                    "- unknown: If the intent is unclear\n\n" +
                    "Key distinctions:\n" +
                    "- 'account details' or 'configure account' or 'setup account' → account.configure\n" +
                    "- 'create user' or 'add user' → user (NOT account.configure)\n" +
                    "- 'data' or 'schema' → registry (data models)\n" +
                    "- 'id' or 'code generation' → idgen (unique identifiers)\n" +
                    "- 'process' or 'flow' → workflow (business processes)\n\n" +
                    "Return ONLY the intent name, nothing else."
                ),
                Map.of(
                    "role", "user",
                    "content", message
                )
            )
        );

        Request request = new Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    mapper.writeValueAsBytes(body),
                    MediaType.parse("application/json")
                )
            )
            .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();

        return mapper
            .readTree(responseBody)
            .path("choices").get(0)
            .path("message")
            .path("content")
            .asText()
            .trim()
            .toLowerCase();
    }

    /**
     * Simple keyword-based fallback for intent inference
     */
    private String inferIntentSimple(String message) {
        String msg = message.toLowerCase();
        if (msg.contains("start") || msg.contains("setup"))
            return "bootstrap";

        if (msg.contains("assign")) return "role.assign";
        
        // More specific matching for idgen
        if (msg.contains("unique") && (msg.contains("id") || msg.contains("code") || msg.contains("number")))
            return "idgen";
        if (msg.contains("idgen") || msg.contains("id generate"))
            return "idgen";
            
        if (msg.contains("workflow")) return "workflow";
        if (msg.contains("boundary")) return "boundary";
        if (msg.contains("notification")) return "notification";
        if (msg.contains("registry")) return "registry";
        if (msg.contains("user")) return "user";
        if (msg.contains("role")) return "role";

        return "unknown";
    }

    /**
     * Map an intent to the corresponding tool name
     */
    private String mapIntentToTool(String intent, List<String> allowedTools) {
        return switch (intent) {
            case "idgen" -> "idgen.configure";
            case "workflow" -> "workflow.configure";
            case "boundary" -> "boundary.configure";
            case "notification" -> "notification.configure";
            case "registry" -> "registry.configure";
            case "user" -> "user.create";
            case "role" -> "role.create";
            case "role.assign" -> "role.assign";
            default -> null;
        };
    }

    /**
     * Get a human-readable description of what the intent means
     */
    private String getIntentDescription(String intent) {
        return switch (intent) {
            case "idgen" -> "configure unique ID generation";
            case "workflow" -> "configure workflows";
            case "boundary" -> "configure boundaries";
            case "notification" -> "configure notifications";
            case "registry" -> "configure registry schemas";
            case "user" -> "create a user";
            case "role" -> "create a role";
            case "role.assign" -> "assign a role to a user";
            default -> "proceed";
        };
    }

    private String callOpenAi(String userMessage, List<String> allowedTools)
            throws Exception {

        String prompt = buildPrompt(userMessage, allowedTools);

        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
            "temperature", 0,
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content",
                    "You are a strict tool selector. " +
                    "Return exactly ONE tool name from the allowed list. " +
                    "No explanation. No extra text."
                ),
                Map.of(
                    "role", "user",
                    "content", prompt
                )
            )
        );

        Request request = new Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    mapper.writeValueAsBytes(body),
                    MediaType.parse("application/json")
                )
            )
            .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();

        return mapper
            .readTree(responseBody)
            .path("choices").get(0)
            .path("message")
            .path("content")
            .asText()
            .trim();
    }

    private String buildPrompt(String userMessage, List<String> allowedTools) {
        return """
User request:
%s

Allowed tools:
%s

Tool meanings:
- account.create → create DIGIT account
- account.configure → authenticate account
- idgen.configure → configure unique ID generation
- workflow.configure → configure workflows
- notification.configure → configure notifications
- boundary.configure → configure boundaries
- registry.configure → configure registry schemas
- user.create → create a user
- role.create → create a role
- role.assign → assign role to user

Rules:
- Choose the best matching tool
- Return ONLY the tool name
""".formatted(userMessage, allowedTools);
    }
}
