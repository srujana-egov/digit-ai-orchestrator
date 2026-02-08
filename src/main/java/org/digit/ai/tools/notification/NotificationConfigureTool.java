package org.digit.ai.tools.notification;

import org.digit.ai.state.ConfigState;
import org.digit.ai.tools.ToolHandler;

public class NotificationConfigureTool implements ToolHandler {

    @Override
    public String name() {
        return "notification.configure";
    }

    @Override
    public void execute(ConfigState state) {
        state.setNotificationConfigured(true);
    }
}
