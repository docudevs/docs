package examples.contract_analysis.generate_profile;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id of the case that holds the playbook documents
        JsonNode generation = client.generateContractAnalysisProfile(caseId, "Acme Corp", "Globex Ltd");
        System.out.println("Profile generation job: " + generation.path("jobGuid").asText());
    }
}
// docs:end
