package examples.contract_analysis.publish_profile;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        long draftRevision = 2L; // draftRevision returned by the last draft update
        Set<String> acknowledgedWarningCodes = Set.of(); // List here the warning codes from the readiness or publish response that you accept.

        JsonNode published = client.publishContractAnalysisProfile(
                caseId, draftRevision, acknowledgedWarningCodes);
        System.out.println("Published revision: " + published.path("publishedRevision").asLong());
        // docs:end
    }
}
