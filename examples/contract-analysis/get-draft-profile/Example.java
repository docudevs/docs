package examples.contract_analysis.get_draft_profile;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        JsonNode draft = client.getContractAnalysisProfile(caseId, "draft");

        System.out.println("Status: " + draft.path("status").asText());
        System.out.println("Draft revision: " + draft.path("draftRevision").asLong());
        // docs:end
    }
}
