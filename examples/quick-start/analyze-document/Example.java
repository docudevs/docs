package examples.quick_start.analyze_document;

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
        byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

        String ocrJobId = client.uploadDocument(
                new UploadRequest("your-document.pdf", "application/pdf", documentData));
        client.ocrDocument(
                ocrJobId,
                ProcessOptions.builder().ocr("PREMIUM").mimeType("application/pdf").build(),
                "markdown");
        client.waitUntilReadyText(
                ocrJobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        // One call submits the generative task and waits for it; it returns the task's result document
        JsonNode analysis = client.submitAndWaitForGenerativeTask(
                ocrJobId,
                "Summarize this document and explain what type of document it is and its main purpose.",
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());

        System.out.println("\nAI Analysis:");
        System.out.println(analysis.path("generated_text").asText());
        // docs:end
    }
}
