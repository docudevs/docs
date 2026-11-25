package examples.contract_analysis.compare_generation;

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
        String generationJobGuid = "<generation-job-guid>"; // jobGuid returned by the generate request

        JsonNode comparison = client.getContractAnalysisGenerationComparison(caseId, generationJobGuid);

        System.out.println("Classification: " + comparison.path("classification").asText());
        System.out.println("Current draft revision: " + comparison.path("currentDraftRevision").asLong());
        System.out.println("Elements to review: " + comparison.path("elements").size());
        // docs:end
    }
}
