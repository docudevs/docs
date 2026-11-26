package examples.records.upsert_record;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
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
        JsonNode accepted = client.upsertRecord(
                caseId,
                "1000000000001",
                Map.of("title", "Soft sock 3-pack", "description", "Cotton, size 40-45"),
                "4.12.3",
                null,
                Map.of("brand", "Foo"));
        System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
        // docs:end
    }
}
