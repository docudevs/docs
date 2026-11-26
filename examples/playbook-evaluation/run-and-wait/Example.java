package examples.playbook_evaluation.run_and_wait;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the playbook's case

        // The client creates the idempotency key; pass one as the last argument to retry a start safely
        JsonNode started = client.startEvaluation(caseId, "tuning", null, null);
        String evaluationId = started.path("evaluationId").asText();
        System.out.println("Evaluation " + evaluationId + " started: " + started.path("caseIds").size() + " cases");

        // Polls every 15 seconds and throws DocuDevsTimeoutException after 3600 seconds
        JsonNode finished = client.waitForEvaluation(
                caseId,
                evaluationId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(15))
                        .build());
        System.out.println("Evaluation " + evaluationId + " complete: " + finished.path("trialCounts"));
        // docs:end
    }
}
