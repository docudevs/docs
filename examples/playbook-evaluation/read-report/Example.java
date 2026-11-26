package examples.playbook_evaluation.read_report;

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
        long caseId = 42L; // id of the playbook's case

        JsonNode listing = client.listEvaluations(caseId);
        for (JsonNode evaluation : listing.path("evaluations")) {
            System.out.println(evaluation.path("evaluationId").asText() + " " + evaluation.path("set").asText() + " "
                    + (evaluation.path("complete").asBoolean() ? "complete" : "running"));
        }

        String evaluationId = "<evaluation-id>"; // evaluationId returned when the evaluation was started
        JsonNode status = client.getEvaluation(caseId, evaluationId);
        System.out.println("Trials: " + status.path("trialCounts"));

        // Pass an evaluation id or "published" instead of null to compare with another evaluation
        JsonNode report = client.getEvaluationReport(caseId, evaluationId, null);
        JsonNode totals = report.path("totals");
        System.out.println("Complete: " + report.path("complete").asBoolean());
        System.out.println("Passed " + totals.path("passed").asLong() + " of " + totals.path("assertions").asLong()
                + " assertions");
        System.out.println("Missed problems: " + totals.path("missedProblems").asLong() + ", false alarms: "
                + totals.path("falseAlarms").asLong());
        for (JsonNode contract : report.path("contracts")) {
            if (!contract.path("trialStatus").asText().equals("finished")) {
                System.out.println(contract.path("name").asText() + ": " + contract.path("trialStatus").asText()
                        + " (" + contract.path("failureReason").asText("no reason") + ")");
            }
        }
        // docs:end
    }
}
