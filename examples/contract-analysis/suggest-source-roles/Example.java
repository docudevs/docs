package examples.contract_analysis.suggest_source_roles;

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

        JsonNode suggestion = client.suggestContractAnalysisSourceRoles(caseId);
        System.out.println("Suggestion job: " + suggestion.path("jobGuid").asText());
        // docs:end
    }
}
