# Session Handling & In-Memory Persistence

## ✅ Implemented

Session handling with YES/NO responses is now fully functional.

## How It Works

### 1. ConversationSession
Each session maintains:
- `ConfigState` - tracks what's been configured
- `pendingAction` - remembers what the AI proposed

### 2. SessionStore
- Thread-safe in-memory storage using `ConcurrentHashMap`
- Sessions identified by `X-Session-Id` header
- Default session: "default"

### 3. YES/NO Handling
When user says "yes" or "no":
- **Bypasses AI completely** (deterministic)
- Executes the pending action
- Clears the pending action

### 4. Proposed Actions
When AI explains what it wants to do:
- Stores the proposed action in session
- User can confirm with "yes" or decline with "no"

## Demo Flow

### Example 1: Configure Workflow

```bash
# Step 1: User asks about workflow
curl -X POST localhost:8080/mcp/ai \
  -H "X-Session-Id: demo" \
  -H "Content-Type: application/json" \
  -d '{"message":"configure workflow"}'
```

Response:
```json
{
  "success": false,
  "message": "Before I can configure workflow, I need to create and configure your account first. Shall I proceed with that?"
}
```

```bash
# Step 2: User confirms
curl -X POST localhost:8080/mcp/ai \
  -H "X-Session-Id: demo" \
  -H "Content-Type: application/json" \
  -d '{"message":"yes"}'
```

Response:
```json
{
  "success": true,
  "message": "Executed: account.create"
}
```

### Example 2: Decline Action

```bash
# Step 1: User asks
curl -X POST localhost:8080/mcp/ai \
  -H "X-Session-Id: demo2" \
  -H "Content-Type: application/json" \
  -d '{"message":"i need unique codes"}'
```

Response:
```json
{
  "success": false,
  "message": "Before I can configure idgen, I need to create and configure your account first. Shall I proceed with that?"
}
```

```bash
# Step 2: User declines
curl -X POST localhost:8080/mcp/ai \
  -H "X-Session-Id: demo2" \
  -H "Content-Type: application/json" \
  -d '{"message":"no"}'
```

Response:
```json
{
  "success": true,
  "message": "Okay, let me know what you'd like to do next."
}
```

## Architecture

```
User Request
    ↓
McpController
    ↓
SessionStore.getSession(sessionId)
    ↓
Check if message is "yes" or "no"
    ├─ YES → Execute pending action
    ├─ NO → Clear pending action
    └─ Other → Call AI
        ↓
    AiDecision (EXECUTE or EXPLAIN)
        ↓
    If EXPLAIN → Store proposedAction in session
        ↓
    Response to User
```

## Key Features

✅ **Deterministic YES/NO** - No AI involved, instant response
✅ **Session isolation** - Each session has independent state
✅ **Thread-safe** - ConcurrentHashMap for concurrent requests
✅ **Simple** - No chat history, just "what were we about to do?"
✅ **Replaceable** - Easy to swap with Redis later

## Session Header

All requests should include:
```
X-Session-Id: your-session-id
```

If omitted, defaults to "default" session.

## Testing

All existing tests pass (165 tests):
- ✅ Intent inference (148 tests)
- ✅ Tool selection (7 tests)
- ✅ Allowed tools resolution (5 tests)
- ✅ Orchestrator (4 tests)
- ✅ Application context (1 test)

## Next Steps

- ✅ Intent inference
- ✅ Session handling (YES/NO)
- ✅ In-memory persistence
- ⏭️ Demo-ready behavior (polish UX)
- ⏭️ Redis persistence (optional)
