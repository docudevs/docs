package examples.extraction.reprocess_with_map_reduce;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
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

        ProcessOptions options = ProcessOptions.builder()
                .prompt("Extract all line items (sku, description, qty, total)")
                .schema(Schema.arrayOf(Map.of("type", "object")))
                .mapReduce(MapReduceOptions.builder()
                        .pagesPerChunk(5)
                        .overlapPages(1)
                        .dedupKey("sku")
                        .parallelProcessing(true)
                        .build())
                .build();

        // The Java client has no dependsOn option, which the Python and cURL variants send
        client.processDocument(existingJobGuid, options);

        JsonNode result = client.waitUntilReadyJson(existingJobGuid, WaitOptions.defaults());
        System.out.println("Header: " + result.path("header"));
        System.out.println("Extracted " + result.path("records").size() + " line items.");
        // docs:end
    }
}
