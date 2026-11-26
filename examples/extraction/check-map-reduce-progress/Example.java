package examples.extraction.check_map_reduce_progress;

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
        String jobId = "<job-guid>"; // GUID of a running map-reduce job

        // The Java client throws DocuDevsApiException when the status is not 200
        JsonNode status = client.getJobStatus(jobId);

        JsonNode progress = status.path("mapReduceStatus");
        if (!progress.isMissingNode() && !progress.isNull()) {
            System.out.println("Progress: " + progress.path("completedChunks").asInt()
                    + " / " + progress.path("totalChunks").asInt());
        }
        // docs:end
    }
}
