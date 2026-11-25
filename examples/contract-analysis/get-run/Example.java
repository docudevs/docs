package examples.contract_analysis.get_run;

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
        JsonNode run = client.getContractAnalysisRun(runId);

        System.out.println("Status: " + run.path("status").asText());
        System.out.println("Result available: " + run.path("resultAvailable").asBoolean());
        // docs:end
    }
}
