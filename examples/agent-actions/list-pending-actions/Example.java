package examples.agent_actions.list_pending_actions;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id returned when the case was created

        JsonNode pending = client.listAgentActions(caseId, "pending", 20, null);
        for (JsonNode action : pending.path("items")) {
            System.out.println(action.path("id").asText() + " " + action.path("tool").asText()
                    + " " + action.path("summary").asText());
        }
        // docs:end
    }
}
