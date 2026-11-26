package examples.llm_providers.update_llm_provider;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.LlmProviderUpdate;
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

        JsonNode provider = client.updateLlmProvider(
                providerId,
                LlmProviderUpdate.builder().deploymentName("gpt-4o-mini").build());
        System.out.println(provider.toPrettyString());
        // docs:end
    }
}
