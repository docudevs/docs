package examples.cases.demote_knowledge_base;

import ai.docudevs.client.DocuDevsClient;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a knowledge base

        // Returns nothing; an error status throws DocuDevsApiException
        client.deleteKnowledgeBase(caseId);

        System.out.println("Case " + caseId + " is no longer a knowledge base");
        // docs:end
    }
}
