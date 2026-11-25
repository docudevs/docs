package examples.contract_analysis.list_run_reviews;

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
        String runId = "<run-id>"; // runId returned when the run was created
        JsonNode reviews = client.getContractAnalysisRunReviews(runId);

        System.out.println(reviews.toPrettyString());
        // docs:end
    }
}
