package examples.contract_analysis.get_published_profile;

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
        JsonNode active = client.getContractAnalysisProfile(caseId, "published");

        System.out.println("Status: " + active.path("status").asText());
        System.out.println("Published revision: " + active.path("publishedRevision").asLong());
        // docs:end
    }
}
