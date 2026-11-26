package examples.classification.start_classify;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
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

        List<Map<String, Object>> rows = List.of(
                Map.of(
                        "key", "1000000000003",
                        "text", Map.of("title", "Running socks 3-pack", "description", "Cotton blend")));

        // Throws DocuDevsApiException when the request is refused
        JsonNode accepted = client.classify(
                caseId,
                rows,
                ClassifyOptions.builder()
                        .target("category")
                        .llmLevel("MINI")
                        .targetDepth(List.of(4, 5))
                        .build());
        System.out.println("Classify job " + accepted.path("jobId").asText() + ": " + accepted.path("status").asText());
        // docs:end
    }
}
