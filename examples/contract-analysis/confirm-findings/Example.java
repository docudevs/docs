package examples.contract_analysis.confirm_findings;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        String runId = "<run-id>"; // runId returned when the run was created
        String resultFingerprint = "<result-fingerprint>"; // resultFingerprint of that run's result
        long suiteRevision = 4L; // revision returned by the test-suite read

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode confirmation = mapper.createObjectNode()
                .put("runId", runId)
                .put("resultFingerprint", resultFingerprint)
                .put("expectedSuiteRevision", suiteRevision);
        ObjectNode clause = confirmation.putArray("confirmations").addObject()
                .put("clauseId", "term")
                .put("expectedVerdict", "flagged")
                .put("importance", "mandatory")
                .put("notes", "Confirmed by review.");
        clause.putObject("expectedRedFlags").put("auto-renewal", "triggered");

        JsonNode confirmed = client.confirmContractAnalysisFindings(caseId, confirmation);
        System.out.println("Suite case: " + confirmed.path("suiteCaseId").asText());
        // docs:end
    }
}
