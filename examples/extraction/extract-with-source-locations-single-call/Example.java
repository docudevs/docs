package examples.extraction.extract_with_source_locations_single_call;

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
                        "clauses", Map.of("type", "array", "items", Map.of("type", "string"))),
                "required", List.of("clauses")));

        byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));

        JsonNode combined = client.submitAndWaitForDocumentWithSourceLocations(
                new UploadRequest("contract.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract the main clauses from this contract.")
                        .schema(schema)
                        .sourceLocationGranularity("block")
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        System.out.println(combined.path("result").toPrettyString());
        System.out.println(combined.path("sourceLocations").toPrettyString());
        // docs:end
    }
}
