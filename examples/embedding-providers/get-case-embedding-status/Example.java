package examples.embedding_providers.get_case_embedding_status;

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
        // caseId is the id of an organization-wide case.
        long caseId = 3L;

        JsonNode status = client.getCaseEmbeddingStatus(caseId);
        JsonNode active = status.path("active");
        JsonNode candidate = status.path("candidate");
        System.out.println("Active dimensions: " + (active.isObject() ? active.path("dimensions").asText() : null));
        System.out.println("Candidate status: " + (candidate.isObject() ? candidate.path("status").asText() : null));
        // docs:end
    }
}
