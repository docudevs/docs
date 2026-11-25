package examples.contract_analysis.create_trial;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        String anchorJobGuid = "<completed-contract-job>"; // jobGuid of a completed contract job
        long suiteRevision = 3L; // revision returned by the test-suite read

        JsonNode estimate = client.estimateContractAnalysisTrial(
                caseId, anchorJobGuid, suiteRevision, "scored", null, null);

        JsonNode trial = client.createContractAnalysisTrial(
                caseId,
                anchorJobGuid,
                "trial-key",
                suiteRevision,
                "scored",
                null,
                estimate.path("estimateFingerprint").asText(),
                estimate.path("executionFingerprint").asText());
        System.out.println("Trial ID: " + trial.path("trialId").asText());
        // docs:end
    }
}
