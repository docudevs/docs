package examples.agent_runs.list_runs;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.AgentRunListOptions;
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
        long classifyJobId = 55L; // id of a classify job of this case

        JsonNode runs = client.listAgentRuns(
                caseId,
                AgentRunListOptions.builder().originJobId(classifyJobId).status("COMPLETED").build());
        for (JsonNode run : runs.path("items")) {
            System.out.println(run.path("runId").asText() + " " + run.path("status").asText()
                    + " " + run.path("tokensUsed").asText());
        }
        // docs:end
    }
}
