package examples.extraction.reprocess_with_map_reduce;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String existingJobGuid = "<job-guid>"; // GUID of a completed job from a previous run

        // Re-run with map-reduce on the same document: no upload, no new OCR
        JsonNode result = client.submitAndWaitForMapReduce(
                existingJobGuid,
                ProcessOptions.builder()
                        .prompt("Extract all line items (sku, description, qty, total)")
                        .schema(Schema.arrayOf(Map.of("type", "object")))
                        .build(),
                MapReduceOptions.builder()
                        .pagesPerChunk(5)
                        .overlapPages(1)
                        .dedupKey("sku")
                        .parallelProcessing(true)
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(300))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        System.out.println("Header: " + result.path("header"));
        System.out.println("Extracted " + result.path("records").size() + " line items.");
        // docs:end
    }
}
