package examples.classification.wait_for_classify;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        long jobId = 9L; // jobId returned when the job was started

        // Throws DocuDevsApiException (409) when the job fails or is cancelled, and
        // DocuDevsTimeoutException after 3600 seconds
        JsonNode job = client.waitForClassify(
                caseId,
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(2))
                        .build(),
                current -> System.out.println(current.path("status").asText() + " " + current.path("counts")));
        System.out.println(job.path("status").asText() + " " + job.path("counts") + " " + job.path("usage"));
        // docs:end
    }
}
