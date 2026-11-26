package examples.ocr.extract_text;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] documentData = Files.readAllBytes(Path.of("document.pdf"));

        String jobId = client.uploadDocument(
                new UploadRequest("document.pdf", "application/pdf", documentData));
        client.ocrDocument(
                jobId,
                ProcessOptions.builder().ocr("DEFAULT").mimeType("application/pdf").build(),
                "markdown");

        String text = client.waitUntilReadyText(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(text);
        // docs:end
    }
}
