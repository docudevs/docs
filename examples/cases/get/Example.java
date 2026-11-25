package examples.cases.get;

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
        JsonNode caseDetails = client.getCase(caseId);

        System.out.println("Case: " + caseDetails.path("name").asText());
        System.out.println("Description: " + caseDetails.path("description").asText());
        System.out.println("Created: " + caseDetails.path("createdAt").asText());
        System.out.println("Organization ID: " + caseDetails.path("organizationId").asLong());
        // docs:end
    }
}
