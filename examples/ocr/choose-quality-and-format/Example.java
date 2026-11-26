package examples.ocr.choose_quality_and_format;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
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
        byte[] documentData = Files.readAllBytes(Path.of("handwritten.pdf"));

        // ocr: "DEFAULT", "PREMIUM" or "AUTO". format: "markdown" or "plain"
        String jobId = client.uploadDocument(
                new UploadRequest("handwritten.pdf", "application/pdf", documentData));
        client.ocrDocument(
                jobId,
                ProcessOptions.builder().ocr("PREMIUM").mimeType("application/pdf").build(),
                "plain");

        String text = client.waitUntilReadyText(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(text);
        // docs:end
    }
}
