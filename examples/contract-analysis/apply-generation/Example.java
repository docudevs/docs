package examples.contract_analysis.apply_generation;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        String generationJobGuid = "<generation-job-guid>"; // jobGuid returned by the generate request

        JsonNode comparison = client.getContractAnalysisGenerationComparison(caseId, generationJobGuid);

        JsonNode decisions = new ObjectMapper().readTree("{\"hours\": {\"action\": \"accept_proposed\"}}");
        JsonNode applied = client.applyContractAnalysisGeneration(
                caseId,
                generationJobGuid,
                comparison.path("currentDraftRevision").asLong(),
                comparison.path("currentDraftFingerprint").asText(),
                decisions);
        System.out.println("New draft revision: " + applied.path("draftRevision").asLong());
        // docs:end
    }
}
