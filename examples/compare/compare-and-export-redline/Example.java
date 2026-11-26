package examples.compare.compare_and_export_redline;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String compareGuid = client.compareDocuments(
                new UploadRequest("slip-2025.pdf", "application/pdf", Files.readAllBytes(Path.of("slip-2025.pdf"))),
                new UploadRequest("slip-2026-draft.pdf", "application/pdf",
                        Files.readAllBytes(Path.of("slip-2026-draft.pdf"))),
                "PREMIUM",
                "Slip 2025",
                "Draft 2026");

        // Throws DocuDevsApiException with the failed stage as its body if a stage fails,
        // and DocuDevsTimeoutException after 1200 seconds
        JsonNode artifact = client.waitUntilCompareReady(
                compareGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(1200))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println("Changes found: " + artifact.path("changes").size());

        // Export a redline as PDF with only the substantive changes
        byte[] pdfBytes = client.exportDocumentCompare(
                compareGuid,
                "pdf",
                Map.of("substantiveOnly", true),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(300))
                        .pollInterval(Duration.ofSeconds(1))
                        .build());
        Files.write(Path.of("redline.pdf"), pdfBytes);
        System.out.println("Saved " + pdfBytes.length + " bytes to redline.pdf");
        // docs:end
    }
}
