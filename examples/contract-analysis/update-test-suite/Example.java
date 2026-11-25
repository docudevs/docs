package examples.contract_analysis.update_test_suite;

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
        JsonNode suite = client.getContractAnalysisTestSuite(caseId);

        JsonNode cases = suite.path("cases"); // add or edit suite cases here

        JsonNode updated = client.updateContractAnalysisTestSuite(
                caseId, suite.path("revision").asLong(), cases);
        System.out.println("Suite revision: " + updated.path("revision").asLong());
        // docs:end
    }
}
