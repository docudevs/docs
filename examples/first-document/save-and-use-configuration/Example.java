package examples.first_document.save_and_use_configuration;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String configName = "invoice_processing_standard";

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("invoice_number", Map.of("type", "string"));
        properties.put("date", Map.of("type", "string", "format", "date"));
        properties.put("vendor", Map.of("type", "string"));
        properties.put("total", Map.of("type", "number"));

        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("total")));

        // Save the configuration under a name
        client.saveConfiguration(configName, ProcessOptions.builder()
                .prompt("Extract comprehensive invoice data")
                .schema(schema)
                .llm("HIGH")
                .build());
        System.out.println("Configuration saved: " + configName);

        // Upload a document and process it with the saved configuration
        byte[] documentData = Files.readAllBytes(Path.of("sample_invoice.pdf"));

        String jobId = client.submitAndProcessDocumentWithConfiguration(
                new UploadRequest("sample_invoice.pdf", "application/pdf", documentData),
                configName);

        JsonNode result = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println("Processing with Named Configuration Result:");
        System.out.println(result.toPrettyString());
        // docs:end
    }
}
