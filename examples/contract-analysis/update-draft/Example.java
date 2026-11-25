package examples.contract_analysis.update_draft;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        JsonNode draft = client.getContractAnalysisProfile(caseId, "draft");

        JsonNode profile = draft.path("profile"); // edit the profile here

        JsonNode updated = client.updateContractAnalysisDraft(
                caseId,
                draft.path("draftRevision").asLong(),
                profile,
                "Reviewed clause wording");
        System.out.println("Draft revision: " + updated.path("draftRevision").asLong());
        // docs:end
    }
}
