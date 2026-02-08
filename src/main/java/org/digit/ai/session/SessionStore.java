package org.digit.ai.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {

    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public ConversationSession getSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> new ConversationSession());
    }
}
