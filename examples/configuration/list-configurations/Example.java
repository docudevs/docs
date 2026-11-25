package examples.configuration.list_configurations;

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
        JsonNode configs = client.listConfigurations();

        for (JsonNode config : configs) {
            System.out.println("Name: " + config.path("name").asText() + ", Created: " + config.path("createdAt").asText());
        }
        // docs:end
    }
}
