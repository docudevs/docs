package examples.records.retry_import;

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
        long importId = 301L; // id of the import that failed

        // Throws DocuDevsApiException (409) when the import has not failed
        JsonNode accepted = client.retryImport(caseId, importId);
        System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
        // docs:end
    }
}
