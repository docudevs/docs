package examples.templates.acroform_job_metadata;

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
        byte[] pdfBytes = Files.readAllBytes(Path.of("fillable-form.pdf"));

        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("fillable-form.pdf", "application/pdf", pdfBytes),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .acroFormMetadata(true)
                        .sourceLocations(true)
                        .sourceLocationGranularity("block")
                        .build());
        client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        JsonNode jobMetadata = client.getAcroformMetadata(jobGuid);
        JsonNode sourceLocations = client.getSourceLocations(jobGuid);

        System.out.println("Async metadata fields: " + jobMetadata.path("fields").size());
        System.out.println(sourceLocations.toPrettyString());
        // docs:end
    }
}
