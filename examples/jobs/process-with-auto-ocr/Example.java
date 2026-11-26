package examples.jobs.process_with_auto_ocr;

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
        byte[] documentData = Files.readAllBytes(Path.of("scan.pdf"));

        // AUTO starts with the default OCR and switches to PREMIUM when the quality is low
        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("scan.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract the key fields")
                        .ocr("AUTO")
                        .build());

        client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

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
