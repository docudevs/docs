package examples.contract_analysis.prepare_test_package;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ContractTestPackageOptions;
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
        String anchorJobGuid = "<completed-contract-job>"; // jobGuid of a completed contract job

        JsonNode pkg = client.prepareContractAnalysisTestPackage(
                caseId,
                anchorJobGuid,
                "package-1",
                ContractTestPackageOptions.builder().asOfDate("2026-09-22").build());
        System.out.println("Package " + pkg.path("packageId").asText()
                + ", fingerprint " + pkg.path("fingerprint").asText());
        // docs:end
    }
}
