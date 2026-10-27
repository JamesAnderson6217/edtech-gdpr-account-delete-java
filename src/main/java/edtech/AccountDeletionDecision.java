package edtech;

import java.util.ArrayList;
import java.util.List;

public final class AccountDeletionDecision {
    public enum Status { READY, BLOCKED }

    public record Plan(Status status, List<String> actions, String reason) {}

    public Plan plan(String learnerId, List<String> sessionIds, String keyId) {
        if (learnerId == null || learnerId.isBlank() || keyId == null || keyId.isBlank()) {
            return new Plan(Status.BLOCKED, List.of(), "learner and key identifiers are required");
        }
        List<String> actions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            if (sessionId == null || sessionId.isBlank()) {
                return new Plan(Status.BLOCKED, List.of(), "every session must have an identifier");
            }
            actions.add("revoke-session:" + sessionId);
        }
        actions.add("revoke-account-key:" + keyId);
        return new Plan(Status.READY, List.copyOf(actions), "sessions precede the account key");
    }
}
