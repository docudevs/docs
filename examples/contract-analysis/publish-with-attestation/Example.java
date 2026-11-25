package examples.contract_analysis.publish_with_attestation;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.Set;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        long draftRevision = 2L; // draftRevision of the draft the trial ran against
        String attestationId = "<attestation-id>"; // attestationId returned by the attest request

        JsonNode published = client.publishContractAnalysisProfile(
                caseId, draftRevision, Set.of(), attestationId);
        System.out.println("Published revision: " + published.path("publishedRevision").asLong());
        // docs:end
    }
}
