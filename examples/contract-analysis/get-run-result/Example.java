package examples.contract_analysis.get_run_result;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String runId = "<run-id>"; // runId returned when the run was created
        JsonNode result = client.getContractAnalysisRunResult(runId);

        System.out.println("Result fingerprint: " + result.path("resultFingerprint").asText());
        // docs:end
    }
}
