package examples.contract_analysis.get_source_authority;

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
        JsonNode authority = client.getContractAnalysisSourceAuthority(caseId);

        System.out.println("Revision: " + authority.path("revision").asLong());
        System.out.println("Decisions recorded: " + authority.path("decisions").size());
        // docs:end
    }
}
