package examples.category_trees.activate_tree;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.TreeActivationOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        long versionId = 7L; // treeVersionId returned when the tree was uploaded

        // Throws DocuDevsApiException (409) when the job fails, and DocuDevsTimeoutException after 3600 seconds.
        // The result is the activation import; it carries the job's jobGuid.
        JsonNode job = client.activateTreeAndWait(
                caseId,
                versionId,
                TreeActivationOptions.builder()
                        .successorMapping(Map.of("9.90.1", "4.12.3"))
                        .allowMissing(false)
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println("Activation job " + job.path("jobGuid").asText() + ": " + job.path("status").asText());
        // docs:end
    }
}
