package examples.contract_analysis.list_draft_revisions;

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

        JsonNode log = client.listDraftRevisions(caseId);
        for (JsonNode entry : log.path("revisions")) {
            System.out.println(entry.path("draftRevision").asLong() + " "
                    + entry.path("actorId").asText("-") + " " + entry.path("reason").asText("-"));
        }

        JsonNode earlier = client.getDraftRevision(caseId, 2L);
        System.out.println("Revision " + earlier.path("draftRevision").asLong() + ": " + earlier.path("reason").asText("-"));
        // docs:end
    }
}
