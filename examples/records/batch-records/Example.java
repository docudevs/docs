package examples.records.batch_records;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case

        // Throws DocuDevsApiException when the request is refused
        JsonNode accepted = client.batchRecords(
                caseId,
                List.of(Map.of(
                        "key", "1000000000002",
                        "text", Map.of("title", "Wool sock", "description", "Merino, size 36-40"),
                        "label", "4.12.3",
                        "metadata", Map.of("brand", "Foo"))),
                List.of("1000000000001"));
        System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
        // docs:end
    }
}
