package examples.llm_providers.create_llm_provider;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.LlmProviderOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // credentialId is the "id" of a model credential (see Model credentials).
        long credentialId = 42L;

        JsonNode provider = client.createLlmProvider(
                "Our Azure OpenAI",
                "https://example.openai.azure.com",
                "azure-openai",
                "gpt-4o",
                LlmProviderOptions.builder().credentialId(credentialId).build());
        System.out.println(provider.toPrettyString());
        // docs:end
    }
}
