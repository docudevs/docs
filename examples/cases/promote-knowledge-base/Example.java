package examples.cases.promote_knowledge_base;

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
        long caseId = 42L; // id of an organization-wide case, created with an API key

        JsonNode promoted = client.promoteKnowledgeBase(caseId);

        System.out.println("Case " + promoted.path("id").asLong() + " (" + promoted.path("name").asText()
                + "): knowledge base = " + promoted.path("isKnowledgeBase").asBoolean());
        // docs:end
    }
}
