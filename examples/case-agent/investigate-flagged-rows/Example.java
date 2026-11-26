package examples.case_agent.investigate_flagged_rows;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.AgentChatOptions;
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
        JsonNode status = client.agentChatAndWait(
                "Investigate the flagged rows of classify job 55.",
                AgentChatOptions.builder()
                        .sessionId("week-41")
                        .caseId(42L)
                        .playbook("investigate-flagged")
                        .context("classify:55:17")
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(status.path("response").path("message").asText(null));
        for (JsonNode action : status.path("pendingActions")) {
            System.out.println(action.path("id").asText() + " " + action.path("summary").asText());
        }
        // docs:end
    }
}
