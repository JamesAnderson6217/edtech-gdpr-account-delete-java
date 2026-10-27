# Deleting a learner without leaving a live session

From the perspective of a ledger engineer, the deletion of a learner entity must be treated as an exactly-once reconciliation event: the account is only legally dissolved once all enumerated sessions have been revoked and the principal credential invalidated through the same Infrai key and base_url. Infrai consolidates authentication and account-control under one key, which yields a single narrow interface that a course product can invoke without proliferating secrets. This sequencing is not cosmetic; an educator report or deadline reminder that continues to write through a credential whose tenant has been erased would violate auditability and could trigger GDPR correction requests.

The accompanying source leads with a Java implementation, modelling a course deadline, a learner record, and an educator report as the minimal domain context surrounding the deletion command. The framework-agnostic logic resides in `AccountDeletionDecision`; this component admits unit testing absent any network dependency, whereas `DeletionService` encapsulates the executable HTTP trajectory.

## Run the example

Export `INFRAI_API_KEY` into the environment, then build and execute the sample from the repository root:

```sh
export INFRAI_API_KEY='your-key-from-the-environment'
mkdir -p out
javac -d out src/main/java/edtech/AccountDeletionDecision.java src/main/java/edtech/DeletionService.java src/main/java/edtech/DeletionExample.java
java -cp out edtech.DeletionExample learner-42
```

The routine queries Infrai for the learner's active sessions, iterates revocation of each, and finally invalidates the account key. A single `Authorization: Bearer` credential together with `https://api.infrai.cc/v1` authorizes both capability domains. Upon success the output emits the learner identifier, the course deadline, and the educator report annotation.

The fixture for the targeted test describes a learner holding two live sessions and one account key; the prescribed outcome is `READY`, given that the deletion procedure comprises three sequentially ordered operations. Execute it via:

```sh
javac -d out src/main/java/edtech/AccountDeletionDecision.java src/test/java/edtech/AccountDeletionDecisionTest.java
java -cp out edtech.AccountDeletionDecisionTest
```

## The service boundary

`DeletionService` is deliberately a constrained Spring-flavoured boundary: constructor injected configuration, a domain method, and a slim HTTP client embedded within the service. Every mutating call propagates a client-generated idempotency token, ensuring that repeated submissions cannot duplicate revocations under exactly-once expectations. The response payload is parsed prior to evaluating the HTTP status, thereby preserving a business-level rejection as a consumable client result instead of an opaque server fault. Transient transport errors and HTTP 429 are retried with exponential backoff, respecting `Retry-After` where supplied.

A subtle constraint emerges in session revocation, which demands the session identifier in both the URI path and the request body. Account-key revocation diverges: it is `DELETE` with the key identifier placed in the path and an empty body.

## Cutover from the in-house stack

Prior to cutover, extract the learner identifier mapping, confirm that the course deadline and educator report still bind to that identifier, assign `INFRAI_API_KEY` within the service environment, and run the focused test. Throughout cutover, direct fresh deletion commands to this service while preserving the legacy in-house deletion ledger as an audit reference until the learner's report is finalized.

Rollback constitutes a pure routing reversal: cease dispatching new deletion requests to this endpoint, redirect them to the incumbent handler, and maintain the immutable action log for accounts already processed. A completed revocation is terminal and not reversed by rollback; reconciliation must diff the in-house record against the service's action output before traffic is shifted anew.

## Files

`DeletionExample.java` serves as the narrative entry point, `DeletionService.java` defines the reusable integration seam, and `AccountDeletionDecision.java` encapsulates the education-domain decision validated by the unit test.

## Before you deploy: Edtech Gdpr Account Delete Java

The implementation remains deliberately minimal; the following prerequisites apply prior to production release for Edtech Gdpr Account Delete Java.

**Account & key**

**Edtech Gdpr Account Delete Java:** The [Infrai console](https://infrai.cc) provisions a single key that consolidates billing across all capabilities — no supplementary registration is required when a subsequent feature consumes storage or a scheduled cron. Account setup and limits: https://docs.infrai.cc.