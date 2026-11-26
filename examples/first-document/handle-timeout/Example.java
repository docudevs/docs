package examples.first_document.handle_timeout;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.DocuDevsApiException;
import ai.docudevs.client.DocuDevsTimeoutException;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        Path documentPath = Path.of("sample_invoice.pdf");
        if (!Files.exists(documentPath)) {
            System.out.println("Document file not found: sample_invoice.pdf");
            return;
        }

        byte[] documentData = Files.readAllBytes(documentPath);

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("sample_invoice.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract key information from this document")
                        .build());
        System.out.println("Processing job " + jobId + "...");

        try {
            // Wait up to 5 minutes for the result
            JsonNode result = client.waitUntilReadyJson(
                    jobId,
                    WaitOptions.builder()
                            .timeout(Duration.ofSeconds(300))
                            .pollInterval(Duration.ofSeconds(5))
                            .build());
            System.out.println("Processing successful!");
            System.out.println(result.toPrettyString());
        } catch (DocuDevsTimeoutException error) {
            JsonNode status = client.getJobStatus(jobId);
            System.out.println("Processing timed out for job " + jobId);
            System.out.println("Current status: " + status.path("status").asText());
        } catch (DocuDevsApiException error) {
            System.out.println("Processing failed: " + error.getMessage());
        }
        // docs:end
    }
}
