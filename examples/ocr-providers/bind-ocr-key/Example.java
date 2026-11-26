package examples.ocr_providers.bind_ocr_key;

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

        // List the current OCR key bindings
        JsonNode keys = client.listOcrKeys();
        System.out.println(keys.toPrettyString());

        // Bind the DEFAULT key to the provider; pass null to clear the binding
        client.updateOcrKeyBinding("DEFAULT", providerId);
        // docs:end
    }
}
