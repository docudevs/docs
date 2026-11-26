package examples.case_evals.evaluate_memo_entry;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.MemoEvaluationOptions;
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
        String entryId = "m_88"; // id of a proposed memo entry

        JsonNode started = client.evaluateMemoEntry(
                caseId,
                entryId,
                MemoEvaluationOptions.builder()
                        .sampleSize(200)
                        .seed(7L)
                        .groupField("base_product_id")
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
        for (JsonNode pair : finished.path("report").path("comparisons")) {
            System.out.println(pair.path("baseline") + " " + pair.path("candidate") + " " + pair.path("target"));
        }
        // docs:end
    }
}
