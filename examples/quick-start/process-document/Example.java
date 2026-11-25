package examples.quick_start.process_document;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("your-document.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract all key information from this document")
                        .build());
        System.out.println("Document submitted for processing. Job ID: " + jobId);

        JsonNode data = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println("Processing complete!");

        System.out.println("\nExtracted Data:");
        System.out.println(data.toPrettyString());
    }
}
// docs:end
