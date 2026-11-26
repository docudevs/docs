package examples.agent_runs.investigate_after_classify;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case with an active category tree
        List<String> keys = List.of("SKU-1001", "SKU-1002", "SKU-1003"); // record keys of the case's records

        // Throws DocuDevsApiException (409) when the job fails or is cancelled, and
        // DocuDevsTimeoutException after 3600 seconds
        JsonNode job = client.classifyKeysAndWait(
                caseId,
                keys,
                ClassifyOptions.builder()
                        .maxTotalTokens(400000L)
                        .investigate(Map.of("enabled", true, "maxRows", 10))
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println("Classify job " + job.path("jobId").asText() + " completed; the investigation run starts from it");
        // docs:end
    }
}
