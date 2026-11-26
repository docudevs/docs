package examples.ocr.ocr_with_barcodes;

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
        byte[] documentData = Files.readAllBytes(Path.of("invoice.pdf"));

        // extractionMode "OCR" returns the text only; barcodes(true) turns on the scan
        String jobId = client.submitAndProcessDocument(
                new UploadRequest("invoice.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .ocr("DEFAULT")
                        .extractionMode("OCR")
                        .barcodes(true)
                        .build());

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
