package examples.extraction.split_map_reduce_by_markdown_header;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
        UploadRequest upload = new UploadRequest("contract.pdf", "application/pdf", documentData);

        ProcessOptions options = ProcessOptions.builder()
                .prompt("Extract obligations by section.")
                .mapReduce(MapReduceOptions.builder()
                        .splitType(MapReduceOptions.SplitType.MARKDOWN_HEADER)
                        .splitHeaderLevel(2)
                        .parallelProcessing(true)
                        .build())
                .build();

        String jobId = client.submitAndProcessDocument(upload, options);
        JsonNode result = client.waitUntilReadyJson(jobId, WaitOptions.defaults());
        System.out.println(result.toPrettyString());
        // docs:end
    }
}
