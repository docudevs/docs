package examples.cases.list_knowledge_base;

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
        JsonNode knowledgeBases = client.listKnowledgeBases();

        for (JsonNode knowledgeBase : knowledgeBases) {
            System.out.println("Knowledge base " + knowledgeBase.path("id").asLong() + ": "
                    + knowledgeBase.path("name").asText());
        }
        // docs:end
    }
}
