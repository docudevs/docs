package examples.contract_analysis.attest_trial;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents
        String trialId = "<trial-id>"; // trialId returned when the trial was launched

        JsonNode trial = client.getContractAnalysisTrial(caseId, trialId);

        JsonNode attested = client.attestContractAnalysisTrial(
                caseId,
                trialId,
                trial.path("report").path("reportId").asText(),
                "Reviewed and correct");
        System.out.println("Attestation ID: " + attested.path("attestationId").asText());
        // docs:end
    }
}
