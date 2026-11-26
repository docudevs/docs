package examples.jobs.process_and_clean_up;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("invoice.pdf"));

        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("invoice.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract invoice data")
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        // Save the result locally before deleting the job
        Files.writeString(Path.of(jobGuid + "_result.json"), result.toPrettyString());

        // Clean up the job from DocuDevs
        JsonNode deleteResult = client.deleteJob(jobGuid);
        System.out.println("Cleaned up job: " + deleteResult.path("jobsDeleted").asInt(0) + " deleted");
        // docs:end
    }
}
