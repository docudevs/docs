package examples.templates.fill_pdf_form;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.WaitOptions;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // Retries while a freshly uploaded template finishes preparing
        byte[] filled = client.fillWithRetry(
                "invoice",
                Map.of(
                        "customerName", "Acme Corp",
                        "invoiceNumber", "INV-2024-001",
                        "totalAmount", "1500.00",
                        "paid", true),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(30))
                        .pollInterval(Duration.ofSeconds(1))
                        .build());

        Files.write(Path.of("filled_invoice.pdf"), filled);
        System.out.println("Saved filled_invoice.pdf");
        // docs:end
    }
}
