package examples.ocr_providers.delete_ocr_provider;

import ai.docudevs.client.DocuDevsClient;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // providerId is the "id" returned when the provider was created or listed.
        long providerId = 7L;

        // Returns normally on 204; throws DocuDevsApiException on any other status
        client.deleteOcrProvider(providerId);
        System.out.println("Deleted provider " + providerId);
        // docs:end
    }
}
