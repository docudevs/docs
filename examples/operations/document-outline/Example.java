package examples.operations.document_outline;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.DocumentOutlineOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed document job

        JsonNode outlineResult = client.submitAndWaitForDocumentOutlineOperation(
                jobGuid,
                DocumentOutlineOptions.builder()
                        .pagesPerChunk(3)
                        .overlapPages(1)
                        .parallelProcessing(true)
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        System.out.println(outlineResult.path("result"));
        System.out.println(outlineResult.path("documentOutline"));
        // docs:end
    }
}
