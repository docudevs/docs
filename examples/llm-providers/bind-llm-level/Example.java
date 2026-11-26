package examples.llm_providers.bind_llm_level;

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
        // providerId is the "id" returned when the provider was created or listed.
        long providerId = 7L;

        // List the current bindings of DEFAULT, NANO, MINI and HIGH
        JsonNode keys = client.listLlmKeys();
        System.out.println(keys.toPrettyString());

        // Bind the HIGH level to the provider; pass null to clear a binding
        client.updateLlmKeyBinding("HIGH", providerId);
        // docs:end
    }
}
