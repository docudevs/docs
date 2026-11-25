package examples.contract_analysis.create_draft_run;

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
        String anchorJobGuid = "<completed-contract-job>"; // jobGuid of a completed contract job
        long draftRevision = 4L; // draftRevision returned by the last import or draft save

        JsonNode run = client.createContractAnalysisRun(
                anchorJobGuid, 42L, "draft_trial", draftRevision, "draft-run-1");
        System.out.println("Run ID: " + run.path("runId").asText());
        // docs:end
    }
}
