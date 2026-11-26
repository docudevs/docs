package examples.contract_analysis.record_source_authority_decision;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        JsonNode authority = client.getContractAnalysisSourceAuthority(caseId);

        // Every guideline document of the case must be listed, with the fingerprint read above.
        // This example confirms each document as policy; set the role of each document.
        ObjectNode content = new ObjectMapper().createObjectNode();
        ArrayNode documents = content.putArray("documents");
        for (JsonNode source : authority.path("sources")) {
            documents.addObject()
                    .put("id", source.path("documentId").asText())
                    .put("contentFingerprint", source.path("contentFingerprint").asText())
                    .put("sourceRole", "policy");
        }
        JsonNode decision = client.recordContractAnalysisSourceAuthorityDecision(
                caseId,
                authority.path("revision").asLong(),
                "source_authority",
                content,
                "Reviewer confirmed the governing policy documents");
        System.out.println("Revision: " + decision.path("revision").asLong());
        // docs:end
    }
}
