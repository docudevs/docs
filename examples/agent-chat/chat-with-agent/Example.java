package examples.agent_chat.chat_with_agent;

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
        // Sends the message, polls the status endpoint and throws DocuDevsTimeoutException after 120 seconds
        JsonNode status = client.agentChatAndWait(
                "Help me build an invoice extraction schema",
                AgentChatOptions.builder().sessionId("session-123").build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(status.path("response").path("message").asText(null));
        // docs:end
    }
}
