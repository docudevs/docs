package examples.playbook_evaluation.split_suite;

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
        long caseId = 42L; // id of the playbook's case

        // Both calls raise DocuDevsApiException when the API answers with an error status
        JsonNode suite = client.getContractAnalysisTestSuite(caseId);
        JsonNode saved = client.splitTestSuite(
                caseId,
                new ObjectMapper().createObjectNode()
                        .put("holdout", 0.3)
                        .put("seed", 1)
                        .put("expectedSuiteRevision", suite.path("revision").asLong()));

        int tuning = 0;
        int holdout = 0;
        JsonNode cases = saved.path("cases");
        for (JsonNode testCase : cases) {
            String set = testCase.path("set").asText();
            if (set.equals("tuning")) {
                tuning++;
            } else if (set.equals("holdout")) {
                holdout++;
            }
        }
        System.out.println("Suite revision " + saved.path("revision").asLong() + " saved: " + tuning
                + " tuning, " + holdout + " holdout, " + (cases.size() - tuning - holdout) + " unassigned.");
        // docs:end
    }
}
