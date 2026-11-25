package examples.contract_analysis.propose_test_case;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        String runId = "<run-id>"; // runId returned when the run was created
        String resultFingerprint = "<result-fingerprint>"; // resultFingerprint of that run's result

        JsonNode suite = client.getContractAnalysisTestSuite(caseId);

        ObjectNode proposalRequest = new ObjectMapper().createObjectNode()
                .put("runId", runId)
                .put("resultFingerprint", resultFingerprint)
                .put("clauseId", "hours")
                .put("name", "Hours-clause omission")
                .put("expectedVerdict", "flagged")
                .put("reason", "Reviewer correction")
                .put("expectedSuiteRevision", suite.path("revision").asLong())
                .put("idempotencyKey", "proposal-1");
        JsonNode proposal = client.proposeContractAnalysisFindingTestCase(caseId, proposalRequest);
        System.out.println("Proposed case: " + proposal.path("proposedCaseId").asText());
        // docs:end
    }
}
