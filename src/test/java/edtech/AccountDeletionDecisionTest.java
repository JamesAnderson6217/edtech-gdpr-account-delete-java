package edtech;

import java.util.List;

public final class AccountDeletionDecisionTest {
    public static void main(String[] args) {
        AccountDeletionDecision.Plan plan = new AccountDeletionDecision().plan("learner-42", List.of("session-a", "session-b"), "key-42");
        if (plan.status() != AccountDeletionDecision.Status.READY) throw new AssertionError(plan.reason());
        if (!plan.actions().equals(List.of("revoke-session:session-a", "revoke-session:session-b", "revoke-account-key:key-42"))) {
            throw new AssertionError("session revocation must precede account-key revocation");
        }
        System.out.println("account deletion decision: READY");
    }
}
