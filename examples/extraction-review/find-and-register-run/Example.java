package examples.extraction_review.find_and_register_run;

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
        String jobGuid = "<job-guid>"; // GUID of a job processed with the saved configuration

        JsonNode runs = client.listExtractionReviewRuns(jobGuid);
        for (JsonNode descriptor : runs) {
            System.out.println(descriptor.path("runId").asText() + " "
                    + descriptor.path("availability").asText() + " "
                    + descriptor.path("profileRevision").asText());
        }

        String runId = "<run-id>"; // runId of the descriptor you selected from the list above

        JsonNode registered = client.registerExtractionReviewRun(runId);
        System.out.println(registered.path("identity"));
        System.out.println("Review revision: " + registered.path("reviewRevision").asLong());
        // docs:end
    }
}
