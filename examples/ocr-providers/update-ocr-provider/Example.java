package examples.ocr_providers.update_ocr_provider;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.OcrProviderUpdate;
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

        JsonNode provider = client.updateOcrProvider(
                providerId,
                OcrProviderUpdate.builder().modelId("prebuilt-invoice").build());
        System.out.println(provider.toPrettyString());
        // docs:end
    }
}
