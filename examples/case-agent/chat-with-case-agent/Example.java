package examples.case_agent.chat_with_case_agent;

// docs:start
import ai.docudevs.client.AgentChatOptions;
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        JsonNode status = client.agentChatAndWait(
                "Classify this week file into levels 4 and 5 using the memo rules.",
                AgentChatOptions.builder()
                        .sessionId("week-41")
                        .caseId(42L)
                        .playbook("general")
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
