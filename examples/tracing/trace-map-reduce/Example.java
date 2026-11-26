package examples.tracing.trace_map_reduce;

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
        byte[] documentData = Files.readAllBytes(Path.of("large-report.pdf"));

        // Tracing covers every chunk
        String jobGuid = client.submitAndProcessDocumentMapReduce(
                new UploadRequest("large-report.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .prompt("Extract line items")
                        .trace(true)
                        .build(),
                MapReduceOptions.builder()
                        .pagesPerChunk(5)
                        .build());

        client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        JsonNode trace = client.getTrace(jobGuid);
        System.out.println("Processed in " + trace.path("events").size() + " events");
        // docs:end
    }
}
