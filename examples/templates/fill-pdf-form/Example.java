package examples.templates.fill_pdf_form;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] filled = client.fillTemplate("invoice", Map.of(
            "customerName", "Acme Corp",
            "invoiceNumber", "INV-2024-001",
            "totalAmount", "1500.00",
            "paid", true
        ));

        Files.write(Path.of("filled_invoice.pdf"), filled);
        System.out.println("Saved filled_invoice.pdf");
        // docs:end
    }
}
