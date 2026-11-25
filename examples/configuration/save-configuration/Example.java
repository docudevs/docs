package examples.configuration.save_configuration;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String schema = """
        {
          "type": "object",
          "properties": {
            "vendor_name": {"type": "string"},
            "invoice_number": {"type": "string"},
            "total_amount": {"type": "number"},
            "date": {"type": "string", "format": "date"}
          },
          "required": ["vendor_name", "total_amount"]
        }
        """;

        ProcessOptions options = ProcessOptions.builder()
            .prompt("Extract invoice data including vendor details, line items, and totals.")
            .schema(schema)
            .ocr("PREMIUM")
            .llm("HIGH")
            .extractionMode("STEPS")
            .barcodes(true)
            .build();

        JsonNode saved = client.saveConfiguration("invoice-config", options);
        System.out.println("Saved configuration: " + saved.path("name").asText());
    }
}
// docs:end
