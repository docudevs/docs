package examples.operations.generative_task;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String parentJobGuid = "<job-guid>"; // GUID of a completed document job

        // One call submits the task and waits for it; it returns the task's result document
        JsonNode summary = client.submitAndWaitForGenerativeTask(
                parentJobGuid,
                "Summarize the main findings",
                "DEFAULT",
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(summary.path("generated_text").asText());
        // docs:end
    }
}
