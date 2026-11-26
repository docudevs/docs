package examples.extraction.process_page_range_map_reduce;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));
        UploadRequest upload = new UploadRequest("report.pdf", "application/pdf", documentData);

        // The document has 20 pages, but only pages 1-2, 5-6 and 10-11 are needed.
        // These 6 pages are chunked into 3 chunks of 2 pages each.
        String jobGuid = client.submitAndProcessDocumentMapReduce(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract all items from these pages.")
                        .pageRange(List.of(1, 2, 5, 6, 10, 11))
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
