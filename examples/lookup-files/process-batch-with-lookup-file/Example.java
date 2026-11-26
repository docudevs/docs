package examples.lookup_files.process_batch_with_lookup_file;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String batchGuid = client.createBatch(3);

        for (String filename : List.of("invoice1.pdf", "invoice2.pdf")) {
            byte[] documentData = Files.readAllBytes(Path.of(filename));
            client.uploadBatchDocument(
                    batchGuid,
                    new UploadRequest(filename, "application/pdf", documentData));
        }

        // The batch lookup file overrides the lookup file of invoice-config for this batch
        byte[] lookupData = Files.readAllBytes(Path.of("vendors-2024.csv"));
        client.uploadBatchLookupFile(batchGuid, new UploadRequest("vendors-2024.csv", "text/csv", lookupData));

        client.processBatchWithConfiguration(batchGuid, "invoice-config");

        JsonNode results = client.waitUntilReadyJson(
                batchGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(results.toPrettyString());
        // docs:end
    }
}
