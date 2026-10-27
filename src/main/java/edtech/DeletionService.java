package edtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeletionService {
    private static final String ACCOUNT_KEY_REVOCATION = "account.keys.revoke";
    private final HttpClient client;
    private final String apiKey;
    private final String baseUrl;
    private final AccountDeletionDecision decision = new AccountDeletionDecision();

    public DeletionService(String apiKey, String baseUrl) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String deleteLearner(String learnerId, String keyId) throws IOException, InterruptedException {
        String sessionsEnvelope = request("GET", "/v1/auth/session/list_for_user/" + learnerId, "");
        List<String> sessions = values(sessionsEnvelope, "session_id");
        AccountDeletionDecision.Plan plan = decision.plan(learnerId, sessions, keyId);
        if (plan.status() != AccountDeletionDecision.Status.READY) throw new IllegalStateException(plan.reason());
        for (String sessionId : sessions) {
            String body = "{\"session_id\":\"" + sessionId + "\"}";
            request("POST", "/v1/auth/session/revoke/" + sessionId, body);
        }
        request("DELETE", "/v1/account/keys/revoke/" + keyId, "");
        return "deleted learner " + learnerId + " after " + sessions.size() + " session revocations and account key revocation";
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) throw new IllegalStateException("Infrai rejected request: " + envelope);
            if (response.statusCode() == 429 && attempt < 3) {
                long delay = retryDelay(response, attempt++);
                Thread.sleep(delay);
                continue;
            }
            if (response.statusCode() >= 400 && response.statusCode() < 500) {
                throw new IllegalStateException("Infrai rejected request with HTTP " + response.statusCode() + ": " + envelope);
            }
            if (response.statusCode() >= 500) throw new IOException("upstream transport response " + response.statusCode());
            return envelope;
        }
    }

    private long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong).map(value -> value * 1000L)
                .orElse(250L * (1L << attempt));
    }

    private List<String> values(String envelope, String field) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        while (matcher.find()) result.add(matcher.group(1));
        return result;
    }
}
