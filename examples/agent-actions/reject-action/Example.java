package examples.agent_actions.reject_action;

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
        long actionId = 9002L; // id of a pending action from the case inbox

        JsonNode rejected = client.rejectAgentAction(actionId, "Use the 7.31 subtree instead");
        System.out.println(rejected.path("id").asText() + " " + rejected.path("status").asText());
        // docs:end
    }
}
