package examples.configuration.get_configuration;

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
        JsonNode config = client.getConfiguration("invoice-config");

        System.out.println("Prompt: " + config.path("prompt").asText());
        System.out.println("OCR: " + config.path("ocr").asText());
        System.out.println("LLM: " + config.path("llm").asText());
        // docs:end
    }
}
