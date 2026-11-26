package examples.records.delete_record;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case

        // Throws DocuDevsApiException when the request is refused
        JsonNode accepted = client.deleteRecord(caseId, "1000000000001");
        System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
        // docs:end
    }
}
