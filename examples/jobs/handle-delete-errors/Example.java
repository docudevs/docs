package examples.jobs.handle_delete_errors;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.DocuDevsApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of the job to delete

        try {
            JsonNode result = client.deleteJob(jobGuid);
            System.out.println("Deleted successfully: " + result.path("jobsDeleted").asInt() + " job(s)");
        } catch (DocuDevsApiException error) {
            if (error.getStatusCode() == 404) {
                System.out.println("Job not found - may already be deleted or purged");
            } else if (error.getStatusCode() == 409) {
                List<String> errors = new ArrayList<>();
                new ObjectMapper().readTree(error.getResponseBody())
                        .path("errors")
                        .forEach(message -> errors.add(message.asText()));
                System.out.println("Cannot delete: " + String.join("; ", errors)); // e.g., job still processing
            } else {
                System.out.println("Unexpected status: " + error.getStatusCode());
            }
        }
        // docs:end
    }
}
