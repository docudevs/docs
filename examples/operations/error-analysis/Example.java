package examples.operations.error_analysis;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String jobGuid = "<job-guid>"; // GUID of a completed document job

        client.submitOperation(jobGuid, "error-analysis");

        JsonNode result = client.waitForOperation(
                jobGuid, "error-analysis", WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(result.path("result"));
    }
}
// docs:end
