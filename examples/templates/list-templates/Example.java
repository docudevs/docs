package examples.templates.list_templates;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        JsonNode templates = client.listTemplates();

        for (JsonNode template : templates) {
            System.out.println("Name: " + template.path("name").asText() + ", Created: " + template.path("createdAt").asText());
        }
        // docs:end
    }
}
