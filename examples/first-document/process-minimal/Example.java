package examples.first_document.process_minimal;

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

        byte[] documentBytes = Files.readAllBytes(Path.of("sample_invoice.pdf"));

        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("sample_invoice.pdf", "application/pdf", documentBytes),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());

        System.out.println("Job submitted: " + jobGuid);
        System.out.println(result.toPrettyString());
    }
}
// docs:end
