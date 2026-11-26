package examples.classification.review_classify_row;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyReviewDecision;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        long jobId = 9L; // jobId returned when the job was started
        long rowId = 17L; // rowId of a result row

        JsonNode review = client.reviewClassify(
                caseId,
                jobId,
                rowId,
                ClassifyReviewDecision.correct("4.12.3.1", "Plain cotton sock, no sport use."));
        JsonNode write = review.path("labelWrite");
        System.out.println(review.path("reviewId").asText() + " " + review.path("decision").asText()
                + " " + write.path("importId").asText(null) + " " + write.path("status").asText(null));
        // docs:end
    }
}
