package org.digit.ai.session;

import org.digit.ai.state.ConfigState;

public class ConversationSession {

    private final ConfigState state = new ConfigState();

    // what AI last proposed, waiting for yes/no
    private String pendingAction;

    public ConfigState getState() {
        return state;
    }

    public String getPendingAction() {
        return pendingAction;
    }

    public void setPendingAction(String pendingAction) {
        this.pendingAction = pendingAction;
    }

    public void clearPendingAction() {
        this.pendingAction = null;
    }
}
