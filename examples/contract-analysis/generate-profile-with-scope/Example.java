package examples.contract_analysis.generate_profile_with_scope;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        ObjectNode scope = new ObjectMapper().createObjectNode().put("from", "1 January 2026");
        scope.putArray("jurisdictions").add("Finland");

        JsonNode generation = client.generateContractAnalysisProfile(caseId, "Acme Corp", "Globex Ltd", scope);
        System.out.println("Profile generation job: " + generation.path("jobGuid").asText());
        // docs:end
    }
}
