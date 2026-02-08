# Architecture Explained: Visual Walkthrough

A visual explanation of how the DIGIT AI Orchestrator works.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER REQUEST                             │
│  "I need to configure workflow"                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      McpController                               │
│  - Receives HTTP POST /mcp/ai                                    │
│  - Extracts X-Session-Id header                                  │
│  - Gets or creates session                                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    YES/NO Check (Deterministic)                  │
│                                                                   │
│  if (message == "yes" && pendingAction != null)                  │
│      → Execute pending action                                    │
│      → Clear pending action                                      │
│      → Return success                                            │
│                                                                   │
│  if (message == "no" && pendingAction != null)                   │
│      → Clear pending action                                      │
│      → Return "Okay, let me know..."                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼ (if not yes/no)
┌─────────────────────────────────────────────────────────────────┐
│                   Get Allowed Tools                              │
│                                                                   │
│  orchestrator.getAllowedTools(session.getState())                │
│      ↓                                                            │
│  AllowedToolsResolver.resolve(state)                             │
│      ↓                                                            │
│  Returns: ["account.create"] (if account not created)            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   AI Intent Inference                            │
│                                                                   │
│  aiToolSelector.decide(message, allowedTools)                    │
│      ↓                                                            │
│  1. inferIntentWithAi(message)                                   │
│     → Call OpenAI GPT-4o-mini                                    │
│     → Returns: "workflow"                                        │
│                                                                   │
│  2. Check if configure intent                                    │
│     → Yes, it's a configure intent                               │
│                                                                   │
│  3. Check prerequisites                                          │
│     → allowedTools contains "account.create"                     │
│     → Account not ready!                                         │
│                                                                   │
│  4. Return AiDecision.explain(                                   │
│       "Before I can configure workflow,                          │
│        I need to create your account first.                      │
│        Shall I proceed?",                                        │
│       "account.create"  ← proposed action                        │
│     )                                                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Store Proposed Action                          │
│                                                                   │
│  if (decision.proposedAction() != null)                          │
│      session.setPendingAction("account.create")                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Return Response                             │
│                                                                   │
│  {                                                                │
│    "success": false,                                             │
│    "message": "Before I can configure workflow,                  │
│                I need to create your account first.              │
│                Shall I proceed?"                                 │
│  }                                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow: Complete Conversation

### Request 1: Initial Query

```
USER: "configure workflow"
  ↓
SESSION: demo (new session created)
  ↓
STATE: {
  account: { created: false, configured: false },
  workflowConfigured: false,
  ...
}
  ↓
ALLOWED TOOLS: ["account.create"]
  ↓
AI INFERENCE: "workflow" intent detected
  ↓
PREREQUISITE CHECK: Account not ready
  ↓
DECISION: EXPLAIN with proposedAction="account.create"
  ↓
SESSION UPDATE: pendingAction = "account.create"
  ↓
RESPONSE: "Before I can configure workflow, I need to create your account first. Shall I proceed?"
```

### Request 2: User Confirms

```
USER: "yes"
  ↓
SESSION: demo (existing session)
  ↓
YES/NO CHECK: message == "yes" && pendingAction == "account.create"
  ↓
EXECUTE: orchestrator.execute("account.create", state)
  ↓
STATE UPDATE: {
  account: { created: true, configured: false },  ← CHANGED
  workflowConfigured: false,
  ...
}
  ↓
CLEAR PENDING: pendingAction = null
  ↓
RESPONSE: "Executed: account.create"
```

### Request 3: Next Step

```
USER: "what's next?"
  ↓
SESSION: demo (same session)
  ↓
STATE: {
  account: { created: true, configured: false },
  ...
}
  ↓
ALLOWED TOOLS: ["account.configure"]  ← Changed!
  ↓
AI INFERENCE: "bootstrap" intent
  ↓
DECISION: EXPLAIN with proposedAction="account.configure"
  ↓
RESPONSE: "I need to configure your account. Shall I proceed?"
```

---

## Component Interaction Diagram

```
┌──────────────┐
│   Browser    │
│   (User)     │
└──────┬───────┘
       │ HTTP POST /mcp/ai
       │ X-Session-Id: demo
       │ {"message": "configure workflow"}
       ▼
┌──────────────────────────────────────────────────────────┐
│                    McpController                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 1. Get/Create Session                              │  │
│  │    sessionStore.getSession("demo")                 │  │
│  │    → ConversationSession                           │  │
│  └────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 2. Check YES/NO                                    │  │
│  │    if (message == "yes") → execute pending         │  │
│  │    if (message == "no") → clear pending            │  │
│  └────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 3. Get Allowed Tools                               │  │
│  │    orchestrator.getAllowedTools(state)             │  │
│  └────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 4. Get AI Decision                                 │  │
│  │    aiToolSelector.decide(message, allowedTools)    │  │
│  └────────────────────────────────────────────────────┘  │
│                                                           │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 5. Store Proposed Action                           │  │
│  │    session.setPendingAction(decision.proposed)     │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
       │
       │ {"success": false, "message": "..."}
       ▼
┌──────────────┐
│   Browser    │
└──────────────┘
```

---

## State Machine: Account Setup Flow

```
┌─────────────────┐
│  Initial State  │
│  account:       │
│    created: ❌  │
│    configured:❌│
└────────┬────────┘
         │
         │ User: "configure workflow"
         │ AI: "Need to create account first"
         │ User: "yes"
         ▼
┌─────────────────┐
│ Account Created │
│  account:       │
│    created: ✅  │
│    configured:❌│
└────────┬────────┘
         │
         │ User: "what's next?"
         │ AI: "Need to configure account"
         │ User: "yes"
         ▼
┌─────────────────┐
│Account Configured│
│  account:       │
│    created: ✅  │
│    configured:✅│
└────────┬────────┘
         │
         │ Now all platform features available!
         ▼
┌─────────────────────────────────────────┐
│  Platform Features Available            │
│  - idgen.configure                      │
│  - workflow.configure                   │
│  - boundary.configure                   │
│  - notification.configure               │
│  - registry.configure                   │
│  - user.create                          │
│  - role.create                          │
└─────────────────────────────────────────┘
```

---

## AI Intent Inference Flow

```
┌─────────────────────────────────────────────────────────────┐
│  User Message: "i need unique codes"                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Try AI Inference                                            │
│                                                               │
│  inferIntentWithAi(message)                                  │
│    ↓                                                          │
│  Call OpenAI API:                                            │
│    Model: gpt-4o-mini                                        │
│    Temperature: 0 (deterministic)                            │
│    System Prompt:                                            │
│      "You are an intent classifier.                          │
│       Return ONE intent:                                     │
│       - bootstrap, idgen, workflow, ..."                     │
│    User Message: "i need unique codes"                       │
│    ↓                                                          │
│  OpenAI Response: "idgen"                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Check Intent Type                                           │
│                                                               │
│  isConfigureIntent("idgen") → true                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Check Prerequisites                                         │
│                                                               │
│  allowedTools.contains("account.create") → true              │
│  → Account not ready!                                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Return Decision                                             │
│                                                               │
│  AiDecision.explain(                                         │
│    "Before I can configure idgen,                            │
│     I need to create your account first.                     │
│     Shall I proceed?",                                       │
│    "account.create"                                          │
│  )                                                            │
└─────────────────────────────────────────────────────────────┘
```

### Fallback Flow (if OpenAI fails)

```
┌─────────────────────────────────────────────────────────────┐
│  Try AI Inference                                            │
│    ↓                                                          │
│  OpenAI API Error! (timeout, rate limit, etc.)              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Fallback to Keyword Matching                                │
│                                                               │
│  inferIntentSimple(message)                                  │
│    ↓                                                          │
│  message.toLowerCase()                                       │
│    → "i need unique codes"                                   │
│    ↓                                                          │
│  if (contains("unique") && contains("code"))                 │
│    → return "idgen"                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Continue with same logic                                    │
│  (prerequisite check, decision, etc.)                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Session Isolation

```
┌──────────────────────────────────────────────────────────────┐
│                      SessionStore                             │
│  (ConcurrentHashMap<String, ConversationSession>)            │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Session: "user-1"                                       │ │
│  │   state: { account: { created: true, ... } }           │ │
│  │   pendingAction: "workflow.configure"                   │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Session: "user-2"                                       │ │
│  │   state: { account: { created: false, ... } }          │ │
│  │   pendingAction: "account.create"                       │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Session: "user-3"                                       │ │
│  │   state: { account: { created: true, ... } }           │ │
│  │   pendingAction: null                                   │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘

Each session is completely isolated:
- Independent state
- Independent pending actions
- No cross-contamination
```

---

## Tool Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│  orchestrator.execute("account.create", state)              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  ToolRegistry.get("account.create")                          │
│    ↓                                                          │
│  Returns: AccountCreateTool instance                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  AccountCreateTool.execute(state)                            │
│    ↓                                                          │
│  state.getAccount().setCreated(true)                         │
│  System.out.println("✅ Account created")                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  State Updated                                               │
│                                                               │
│  Before: { account: { created: false } }                     │
│  After:  { account: { created: true } }                      │
└─────────────────────────────────────────────────────────────┘
```

---

## Gating Logic Flow

```
┌─────────────────────────────────────────────────────────────┐
│  AllowedToolsResolver.resolve(state)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  HARD GATE 1: Account Created?                              │
│                                                               │
│  if (!state.getAccount().isCreated())                        │
│      return ["account.create"]                               │
│      ↓                                                        │
│  STOP HERE - Nothing else available                          │
└────────────────────────┬────────────────────────────────────┘
                         │ (if account created)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  HARD GATE 2: Account Configured?                           │
│                                                               │
│  if (!state.getAccount().isConfigured())                     │
│      return ["account.configure"]                            │
│      ↓                                                        │
│  STOP HERE - Platform features not available yet             │
└────────────────────────┬────────────────────────────────────┘
                         │ (if account configured)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Platform Features Available                                 │
│                                                               │
│  tools = []                                                  │
│                                                               │
│  if (!state.isIdGenConfigured())                             │
│      tools.add("idgen.configure")                            │
│                                                               │
│  if (!state.isWorkflowConfigured())                          │
│      tools.add("workflow.configure")                         │
│                                                               │
│  if (!state.getUser().isCreated())                           │
│      tools.add("user.create")                                │
│                                                               │
│  ... (all independent features)                              │
│                                                               │
│  return tools                                                │
└─────────────────────────────────────────────────────────────┘
```

---

## Testing Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Test Pyramid                              │
│                                                               │
│                      ▲                                        │
│                     ╱ ╲                                       │
│                    ╱   ╲                                      │
│                   ╱  E2E ╲  ← 21 Integration Tests           │
│                  ╱───────╲                                    │
│                 ╱         ╲                                   │
│                ╱  Intent   ╲  ← 148 Intent Tests             │
│               ╱─────────────╲                                 │
│              ╱               ╲                                │
│             ╱      Unit       ╲  ← 17 Unit Tests             │
│            ╱───────────────────╲                              │
│                                                               │
│  Total: 186 Tests                                            │
└─────────────────────────────────────────────────────────────┘
```

### Test Coverage Map

```
┌──────────────────────────────────────────────────────────────┐
│  Component              │  Tests  │  Coverage                 │
├──────────────────────────────────────────────────────────────┤
│  AI Intent Inference    │   148   │  ████████████████  95%   │
│  Session Management     │    21   │  ████████████████ 100%   │
│  Tool Selection         │     7   │  ████████████████ 100%   │
│  Gating Logic           │     5   │  ████████████████ 100%   │
│  Orchestrator           │     2   │  ████████████████ 100%   │
│  Tools                  │     2   │  ████████████████ 100%   │
│  Application Context    │     1   │  ████████████████ 100%   │
└──────────────────────────────────────────────────────────────┘
```

---

## Performance Characteristics

### Latency Breakdown

```
Total Request Time: ~500-800ms

┌─────────────────────────────────────────────────────────────┐
│  Component                    │  Time      │  %             │
├─────────────────────────────────────────────────────────────┤
│  Network (user → server)      │  ~50ms     │  ████  10%    │
│  Session lookup               │  ~1ms      │  ▌  0.2%      │
│  YES/NO check                 │  ~0.1ms    │  ▌  0.02%     │
│  Get allowed tools            │  ~1ms      │  ▌  0.2%      │
│  OpenAI API call              │  ~400ms    │  ████████ 80% │
│  Decision logic               │  ~1ms      │  ▌  0.2%      │
│  Store pending action         │  ~0.1ms    │  ▌  0.02%     │
│  Response serialization       │  ~1ms      │  ▌  0.2%      │
│  Network (server → user)      │  ~50ms     │  ████  10%    │
└─────────────────────────────────────────────────────────────┘

Bottleneck: OpenAI API call (80% of time)
```

### Optimization Opportunities

1. **Cache common intents** - Reduce API calls
2. **Use streaming** - Start response before AI completes
3. **Parallel processing** - Check prerequisites while AI thinks
4. **Local model** - For offline/low-latency scenarios

---

## Scalability Considerations

### Current Architecture (In-Memory)

```
┌──────────────────────────────────────────────────────────────┐
│                    Single Server                              │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  SessionStore (ConcurrentHashMap)                      │  │
│  │  - Thread-safe                                         │  │
│  │  - In-memory                                           │  │
│  │  - Lost on restart                                     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
│  Capacity: ~10,000 concurrent sessions                       │
│  Limitation: Single server, no persistence                   │
└──────────────────────────────────────────────────────────────┘
```

### Future Architecture (Redis)

```
┌──────────────────────────────────────────────────────────────┐
│                  Load Balancer                                │
└────────────────────────┬─────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Server 1   │  │  Server 2   │  │  Server 3   │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       └────────────────┼────────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │   Redis Cluster  │
              │  (Session Store) │
              └──────────────────┘

Capacity: Unlimited (horizontal scaling)
Persistence: Yes (survives restarts)
```

---

*This document provides visual explanations of the system architecture and data flows.*
