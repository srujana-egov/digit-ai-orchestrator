package org.digit.ai.state;

import lombok.Data;

@Data
public class AccountState {
    private boolean created;
    private boolean configured;
    private String accessToken;
}
