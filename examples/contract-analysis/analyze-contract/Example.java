package examples.contract_analysis.analyze_contract;

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
        String jobGuid = "<completed-contract-job>"; // jobGuid of a completed contract job
        long knowledgeBaseId = 42L; // id of the case whose profile is published

        // This call polls the contract job's analysis route, so with several analyses of one
        // contract it can return an older one; cURL and Python wait on the analysis job instead
        JsonNode operation = client.submitAndWaitForContractAnalysis(jobGuid, knowledgeBaseId);

        System.out.println(operation.path("result").asText());
        // docs:end
    }
}
