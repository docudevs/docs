package examples.records.get_import;

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
        long importId = 301L; // importId returned when the import was created

        JsonNode current = client.getImport(caseId, importId);
        System.out.println(current.path("status").asText() + " " + current.path("rowsReceived").asText()
                + " " + current.path("rowsRejected").asText());
        System.out.println(current.path("recordsAdded").asText() + " " + current.path("recordsUpdated").asText()
                + " " + current.path("recordsDeleted").asText());
        // docs:end
    }
}
