package examples.cases.update;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 123L; // id returned by createCase
        JsonNode updated = client.updateCase(
                caseId,
                "Q4 2024 Vendor Invoices - Updated",
                "Updated description with additional context");

        System.out.println("Updated case: " + updated.path("name").asText());
        // docs:end
    }
}
