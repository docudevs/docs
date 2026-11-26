package examples.extraction.extract_with_calculated_fields;

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
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "net_amount", Map.of("type", "number"),
                        "vat_rate", Map.of("type", "number"),
                        "gross_amount", Map.of(
                                "type", "number",
                                "x-docudevs", Map.of(
                                        "fieldType", "calculated",
                                        "dependsOn", List.of("net_amount", "vat_rate"),
                                        "formula", Map.of(
                                                "lang", "excel",
                                                "expression", "ROUND({net_amount} * (1 + {vat_rate}), 2)")))),
                "required", List.of("net_amount", "vat_rate", "gross_amount")));

        byte[] documentData = Files.readAllBytes(Path.of("invoice.pdf"));

        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("invoice.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract net amount and vat rate as numbers.")
                        .schema(schema)
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(result.path("gross_amount"));
        // docs:end
    }
}
