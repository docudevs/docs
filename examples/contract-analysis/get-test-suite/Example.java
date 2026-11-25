package examples.contract_analysis.get_test_suite;

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
        JsonNode suite = client.getContractAnalysisTestSuite(caseId);

        System.out.println("Suite revision: " + suite.path("revision").asLong());
        System.out.println("Cases: " + suite.path("cases").size());
        // docs:end
    }
}
