package examples.agent_runs.cancel_run;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id returned when the case was created
        long runId = 9001L; // id returned when the run was started

        JsonNode cancelled = client.cancelAgentRun(caseId, runId);
        System.out.println(cancelled.path("runId").asText() + " " + cancelled.path("status").asText());
        // docs:end
    }
}
