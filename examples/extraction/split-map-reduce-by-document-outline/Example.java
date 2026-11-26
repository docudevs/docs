package examples.extraction.split_map_reduce_by_document_outline;

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

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
        UploadRequest upload = new UploadRequest("contract.pdf", "application/pdf", documentData);

        String jobId = client.submitAndProcessDocumentMapReduce(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract obligations by section.")
                        .build(),
                MapReduceOptions.builder()
                        .splitType(MapReduceOptions.SplitType.DOCUMENT_OUTLINE)
                        .pagesPerChunk(3)
                        .parallelProcessing(true)
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(result.toPrettyString());

        JsonNode outline = client.getDocumentOutline(jobId);
        System.out.println(outline.toPrettyString());
        // docs:end
    }
}
