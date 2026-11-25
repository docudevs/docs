package examples.operations.generative_task;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        ObjectMapper mapper = new ObjectMapper();
        String parentJobGuid = "<job-guid>"; // GUID of a completed document job

        client.createGenerativeTask(parentJobGuid, "Summarize the main findings", "DEFAULT");

        JsonNode result = client.waitForGenerativeTask(
                parentJobGuid, WaitOptions.builder().build());
        JsonNode payload = mapper.readTree(result.path("result").asText());
        System.out.println(payload.path("generated_text").asText());
        // docs:end
    }
}
