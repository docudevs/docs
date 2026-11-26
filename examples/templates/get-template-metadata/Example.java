package examples.templates.get_template_metadata;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        JsonNode metadata = client.waitForTemplateMetadata(
                "invoice",
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(60))
                        .pollInterval(Duration.ofSeconds(1))
                        .build());

        for (JsonNode field : metadata) {
            System.out.println("- " + field.path("name").asText() + " (" + field.path("type").asText() + ")");
        }
        // docs:end
    }
}
