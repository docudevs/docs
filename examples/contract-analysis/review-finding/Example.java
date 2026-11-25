package examples.contract_analysis.review_finding;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String runId = "<run-id>"; // runId returned when the run was created
        String resultFingerprint = "<result-fingerprint>"; // resultFingerprint of that run's result

        ObjectNode reviewRequest = new ObjectMapper().createObjectNode()
                .put("resultFingerprint", resultFingerprint)
                .put("disposition", "corrected")
                .put("correctedVerdict", "flagged")
                .put("expectedReviewRevision", 0)
                .put("idempotencyKey", "review-1")
                .put("reason", "Confirmed the omission against the source clause");
        JsonNode review = client.putContractAnalysisRunReview(runId, "hours", reviewRequest);
        System.out.println("Review revision: " + review.path("reviewRevision").asLong());
        // docs:end
    }
}
