package examples.contract_analysis.record_source_authority_decision;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        JsonNode authority = client.getContractAnalysisSourceAuthority(caseId);

        // "policy-doc" in the decision below must be the id of a real source document in the case
        JsonNode content = new ObjectMapper().readTree("""
                {"documents": [{"id": "policy-doc", "sourceRole": "policy", "excluded": false}]}
                """);
        JsonNode decision = client.recordContractAnalysisSourceAuthorityDecision(
                caseId,
                authority.path("revision").asLong(),
                "source_authority",
                content,
                "Reviewer confirmed the governing policy document");
        System.out.println("Revision: " + decision.path("revision").asLong());
        // docs:end
    }
}
