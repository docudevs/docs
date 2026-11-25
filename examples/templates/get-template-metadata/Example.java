package examples.templates.get_template_metadata;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        JsonNode metadata = client.getTemplateMetadata("invoice");

        for (JsonNode field : metadata) {
            System.out.println("- " + field.path("name").asText() + " (" + field.path("type").asText() + ")");
        }
        // docs:end
    }
}
