package examples.operations.submit_with_llm_type;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed document job

        JsonNode response = client.submitOperation(
                jobGuid,
                "error-analysis",
                "HIGH", // use the high-quality model tier for the analysis
                Map.of());

        System.out.println("Operation job GUID: " + response.path("jobGuid").asText());
        System.out.println("Status: " + response.path("status").asText());
        // docs:end
    }
}
