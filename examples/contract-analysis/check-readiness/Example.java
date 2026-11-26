package examples.contract_analysis.check_readiness;

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

        JsonNode readiness = client.getContractAnalysisTrialReadiness(caseId);

        System.out.println("Ready: " + readiness.path("ready").asBoolean());
        System.out.println("Blockers: " + readiness.path("blockers"));
        System.out.println("Scenarios still missing: " + readiness.path("outstandingScenarios"));
        // docs:end
    }
}
