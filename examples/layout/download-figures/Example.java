package examples.layout.download_figures;

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
        byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

        // extractFigures keeps the figures as images; PREMIUM OCR also renders page images
        String jobId = client.uploadDocument(
                new UploadRequest("report.pdf", "application/pdf", documentData));
        client.ocrDocument(
                jobId,
                ProcessOptions.builder()
                        .ocr("PREMIUM")
                        .mimeType("application/pdf")
                        .extractFigures(true)
                        .build(),
                "markdown");
        client.waitUntilReadyText(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        // List the figures, then download each one
        JsonNode images = client.getFiguresMetadata(jobId).path("images");
        for (JsonNode image : images) {
            String pageIndex = image.hasNonNull("pageIndex") ? image.path("pageIndex").asText() : "unknown";
            System.out.println(image.path("id").asText() + " page " + pageIndex);
        }

        for (JsonNode image : images) {
            String figureId = image.path("id").asText();
            Files.write(Path.of(figureId + ".png"), client.getFigureImage(jobId, figureId));
        }

        // Download the image of the first page (page indexes start at 0)
        Files.write(Path.of("page_0.png"), client.downloadImage(jobId, 0));
        // docs:end
    }
}
