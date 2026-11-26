package examples.embedding_providers.start_case_reindex;

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

        JsonNode candidate = client.startCaseEmbeddingReindex(caseId);
        System.out.println("Candidate generation " + candidate.path("id").asText() + ": " + candidate.path("status").asText());
        // docs:end
    }
}
