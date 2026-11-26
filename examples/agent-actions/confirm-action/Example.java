package examples.agent_actions.confirm_action;

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
        long actionId = 9001L; // id of a pending action from the case inbox

        JsonNode confirmed = client.confirmAgentAction(actionId);
        System.out.println(confirmed.path("status").asText() + " " + confirmed.path("resultStatus").asText(null));

        // A write that answered an error ends as "failed", not "confirmed"
        if (!"confirmed".equals(confirmed.path("status").asText())) {
            throw new IllegalStateException("Action " + actionId + " " + confirmed.path("status").asText()
                    + ": " + confirmed.path("result"));
        }
        // docs:end
    }
}
