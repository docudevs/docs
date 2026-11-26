package examples.extraction.include_current_date_map_reduce;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
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
        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "line_items", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "description", Map.of("type", "string"),
                                                "warranty_end_date", Map.of("type", "string", "format", "date"),
                                                "remaining_warranty_days", Map.of("type", "number")))))));

        byte[] documentData = Files.readAllBytes(Path.of("warranty-list.pdf"));

        // Every chunk receives the current date
        String jobGuid = client.submitAndProcessDocumentMapReduce(
                new UploadRequest("warranty-list.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract line items with remaining warranty days")
                        .schema(schema)
                        .includeCurrentDate(true)
                        .build(),
                MapReduceOptions.builder()
                        .splitType(MapReduceOptions.SplitType.PAGE)
                        .pagesPerChunk(2)
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(result.toPrettyString());
        // docs:end
    }
}
