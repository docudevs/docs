package examples.ocr_providers.create_ocr_provider;

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
        // DOC_INTELLIGENCE_API_KEY holds the key of your Azure Document Intelligence resource.
        JsonNode provider = client.createOcrProvider(
                "Our Doc Intelligence",
                "https://example.cognitiveservices.azure.com",
                System.getenv("DOC_INTELLIGENCE_API_KEY"),
                "prebuilt-document");
        System.out.println(provider.toPrettyString());
        // docs:end
    }
}
