package examples.agent_runs.start_run;

// docs:start
import ai.docudevs.client.AgentRunOptions;
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id returned by create_case
        long classifyJobId = 55L; // id of a completed classify job of this case

        JsonNode started = client.startAgentRun(
                caseId,
                "investigate-flagged",
                AgentRunOptions.builder()
                        .params(Map.of("jobId", classifyJobId, "maxRows", 10))
                        .budget(Map.of("maxTotalTokens", 60000))
                        .build());

        // Throws DocuDevsApiException (409) when the run fails or is cancelled, and
        // DocuDevsTimeoutException after 1800 seconds
        JsonNode run = client.waitForAgentRun(
                caseId,
                started.path("runId").asLong(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(1800))
                        .pollInterval(Duration.ofSeconds(5))
                        .build(),
                status -> System.out.println(status.path("status").asText() + " " + status.path("turnsDone").asText()));
        JsonNode report = run.path("report");
        System.out.println(report.path("summary").asText(null));
        for (JsonNode question : report.path("questions")) {
            System.out.println("question: " + question);
        }
        // docs:end
    }
}
