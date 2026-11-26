package examples.case_evals.run_holdout_eval;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.HoldoutEvalOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id of a records case with an active category tree
        String entryId = "m_88"; // id of a proposed memo entry

        List<Map<String, Object>> variants = List.of(
                Map.of("name", "baseline"),
                Map.of("name", "with-rule", "memoInclude", List.of(entryId)));

        JsonNode started = client.startHoldoutEval(
                caseId,
                variants,
                HoldoutEvalOptions.builder()
                        .sampleSize(200)
                        .seed(7L)
                        .grouping(Map.of("mode", "nearDuplicate", "threshold", 0.98))
                        .config(Map.of("llmLevel", "MINI"))
                        .build());

        // Throws DocuDevsApiException (409) when the evaluation fails, and
        // DocuDevsTimeoutException after 3600 seconds
        JsonNode finished = client.waitForEval(
                caseId,
                started.path("evalId").asLong(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        for (JsonNode variant : finished.path("report").path("variants")) {
            System.out.println(variant.path("name").asText() + " "
                    + variant.path("score").path("targetAccuracy").path("all").asText(null));
        }
        // docs:end
    }
}
