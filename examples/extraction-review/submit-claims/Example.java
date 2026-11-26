package examples.extraction_review.submit_claims;

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
        String runId = "<run-id>"; // runId of the registered run

        JsonNode run = client.getExtractionReviewRun(runId);
        JsonNode identity = run.path("identity");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = (ObjectNode) mapper.readTree("""
                {
                  "claims": [{
                    "claimId": "check-total-0001",
                    "claimRevision": 1,
                    "identity": {
                      "reporterId": "invoice-checker",
                      "externalIssueId": "check-0001",
                      "referenceVersion": "dataset-2026-10",
                      "comparatorVersion": "2"
                    },
                    "address": {"scope": "BASE", "pointer": "/total"},
                    "observed": {"present": true, "valueJson": "0"},
                    "proposedValueJson": "125.5",
                    "rationaleJson": "\\"reference differs from extracted value\\"",
                    "priority": "HIGH"
                  }],
                  "audit": {"evidenceArtifactPaths": [], "exampleEligibility": false}
                }
                """);
        request.putObject("reference")
                .put("jobGuid", identity.path("jobGuid").asText())
                .put("artifactId", identity.path("artifactId").asText())
                .put("resultFingerprint", identity.path("resultFingerprint").asText());
        request.put("expectedReviewRevision", run.path("reviewRevision").asLong());

        JsonNode claim = client.submitExtractionReviewClaims(
                identity.path("runId").asText(), request, "evaluator-2026-10-06-0001");
        System.out.println("Review run: " + claim.path("event").path("runId").asText());
        // docs:end
    }
}
