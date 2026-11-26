package examples.case_evals.compare_evals;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case with an active category tree
        long baselineEvalId = 301L; // ids of two completed holdout evaluations
        long candidateEvalId = 302L; // that drew the same sample

        // Scoring is asynchronous: the answer carries the id of a new evaluation
        JsonNode comparison = client.compareHoldoutEvals(
                caseId, baselineEvalId, candidateEvalId, "baseline", "with-rule");

        // Throws DocuDevsApiException (409) when the scoring fails, and
        // DocuDevsTimeoutException after 3600 seconds
        JsonNode scored = client.waitForEval(
                caseId,
                comparison.path("evalId").asLong(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        for (JsonNode pair : scored.path("report").path("comparisons")) {
            System.out.println(pair.path("baseline") + " " + pair.path("candidate") + " " + pair.path("target"));
        }
        // docs:end
    }
}
