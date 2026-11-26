package examples.jobs.check_quality;

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
        String jobGuid = "<job-guid>"; // GUID of a completed job

        JsonNode job = client.getJobStatus(jobGuid);
        if (job.path("qualityScore").isNumber()) {
            System.out.printf("Quality: %.2f (%s)%n",
                    job.path("qualityScore").asDouble(),
                    job.path("qualityCategory").asText());
        } else {
            System.out.println("No quality score for this job");
        }
        // docs:end
    }
}
