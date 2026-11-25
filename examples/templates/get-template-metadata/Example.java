package examples.templates.get_template_metadata;

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
        // Returns immediately: for a freshly uploaded PDF, repeat the call until the fields appear
        JsonNode metadata = client.getTemplateMetadata("invoice");

        for (JsonNode field : metadata) {
            System.out.println("- " + field.path("name").asText() + " (" + field.path("type").asText() + ")");
        }
        // docs:end
    }
}
