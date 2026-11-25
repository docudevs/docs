package examples.contract_analysis.mark_test_contract;

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
        long caseId = 42L; // id of the case that holds the playbook documents
        String documentId = "<document-id>"; // id of a completed document in the case

        JsonNode updated = client.updateCaseDocument(caseId, documentId, "test_contract", "kept");
        System.out.println("Updated document: " + updated.path("documentId").asText());
        // docs:end
    }
}
