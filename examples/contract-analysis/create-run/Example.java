package examples.contract_analysis.create_run;

import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.generated.model.ContractAnalysisRunRequest;
import ai.docudevs.client.generated.model.ContractPackageMemberRequest;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        ContractAnalysisRunRequest request = new ContractAnalysisRunRequest()
                .anchorJobGuid("<agreement-job>")
                .knowledgeBaseId(42L)
                .mode("published")
                .idempotencyKey("request-1")
                .addPackageMembersItem(new ContractPackageMemberRequest().jobGuid("<agreement-job>").role("agreement"))
                .addPackageMembersItem(new ContractPackageMemberRequest().jobGuid("<amendment-job>").role("amendment"));

        JsonNode created = client.createContractAnalysisRun(request);
        System.out.println("Run ID: " + created.path("runId").asText());
        // docs:end
    }
}
