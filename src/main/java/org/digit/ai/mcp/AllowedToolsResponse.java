package org.digit.ai.mcp;

import java.util.List;

public record AllowedToolsResponse(
        List<String> tools
) {}
