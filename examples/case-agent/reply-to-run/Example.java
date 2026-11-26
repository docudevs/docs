package examples.case_agent.reply_to_run;

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
                "Record that fiction books are filed by genre when the description names one.",
                AgentChatOptions.builder()
                        .sessionId("run-9001-reply")
                        .caseId(42L)
                        .playbook("general")
                        .context("run:9001") // run:{runId}, the id returned when the run was started
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
