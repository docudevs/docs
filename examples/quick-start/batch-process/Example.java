package examples.quick_start.batch_process;

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
        String batchGuid = client.createBatch(3);

        for (String filename : List.of("invoice1.pdf", "invoice2.pdf", "invoice3.pdf")) {
            byte[] documentData = Files.readAllBytes(Path.of(filename));
            client.uploadBatchDocument(
                    batchGuid,
                    new UploadRequest(filename, "application/pdf", documentData));
        }

        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "invoice_number", Map.of("type", "string"),
                        "total", Map.of("type", "number"))));

        client.processBatch(
                batchGuid,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract invoice data")
                        .schema(schema)
                        .build());

        JsonNode results = client.waitUntilReadyJson(
                batchGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).isNull()) {
                System.out.println("Document " + i + ": no result (it failed or has no stored result)");
            } else {
                System.out.println("Document " + i + ": " + results.get(i));
            }
        }
        // docs:end
    }
}
