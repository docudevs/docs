package examples.quick_start.custom_schema;

import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

        // docs:start
        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "invoice_number", Map.of("type", "string"),
                        "date", Map.of("type", "string", "format", "date"),
                        "vendor", Map.of("type", "string"),
                        "total", Map.of("type", "number"),
                        "currency", Map.of("type", "string")),
                "required", List.of("invoice_number", "total")));

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("your-document.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract invoice data according to the schema")
                        .schema(schema)
                        .build());

        JsonNode data = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(data.toPrettyString());
        // docs:end
    }
}
