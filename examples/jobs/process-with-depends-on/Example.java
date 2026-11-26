package examples.jobs.process_with_depends_on;

import ai.docudevs.client.DocuDevsClient;

// docs:start
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
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String headerSchema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "invoice_number", Map.of("type", "string"),
                        "vendor", Map.of("type", "string"),
                        "total", Map.of("type", "number")),
                "required", List.of("total")));

        String lineItemsSchema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "line_items", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "description", Map.of("type", "string"),
                                                "amount", Map.of("type", "number")))))));

        WaitOptions waitOptions = WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build();

        byte[] documentData = Files.readAllBytes(Path.of("invoice.pdf"));

        // First extraction: parse the document and extract the header fields
        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("invoice.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract invoice header fields")
                        .schema(headerSchema)
                        .build());
        JsonNode headerResult = client.waitUntilReadyJson(jobGuid, waitOptions);
        System.out.println(headerResult.toPrettyString());

        // Second extraction: reuse the parsed document with another schema (no re-OCR)
        client.processDocument(
                jobGuid,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract line items")
                        .schema(lineItemsSchema)
                        .dependsOn(jobGuid)
                        .build());
        JsonNode lineItemsResult = client.waitUntilReadyJson(jobGuid, waitOptions);
        System.out.println(lineItemsResult.toPrettyString());
        // docs:end
    }
}
