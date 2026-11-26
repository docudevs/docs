package examples.category_trees.get_activation_report;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        long versionId = 7L; // treeVersionId returned when the tree was uploaded
        String jobGuid = "<job-guid>"; // jobGuid returned when the version was activated

        // Returns when the job is COMPLETED. Throws DocuDevsApiException (409) when it ends ERROR, FAILED or TIMEOUT,
        // and DocuDevsTimeoutException after 600 seconds
        client.waitForRecordsJob(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        // Find the activation among the case's imports (newest first). This reads one page
        // of up to 200; an older activation may need the continuation token of the page.
        JsonNode page = client.listImports(caseId, 200, null);
        Long importId = null;
        for (JsonNode item : page.path("items")) {
            if (jobGuid.equals(item.path("jobGuid").asText(null))) {
                importId = item.path("importId").asLong();
                break;
            }
        }
        if (importId == null) {
            throw new IllegalStateException("No import found for job " + jobGuid);
        }

        JsonNode report = client.getTreeActivationReport(caseId, versionId, importId);
        System.out.println(report.path("status").asText(null) + " " + report.path("counts"));
        System.out.println("Memo entries to review: " + report.path("memoScopeIssues").size());
        // docs:end
    }
}
