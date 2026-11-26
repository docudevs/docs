package examples.model_credentials.create_api_key_credential;

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
        // PROVIDER_API_KEY holds the API key issued by your model provider.
        // Java: the call returns the JSON body of the 201 response and throws DocuDevsApiException on an error status.
        JsonNode credential = client.createModelCredential("Provider embeddings", System.getenv("PROVIDER_API_KEY"));
        System.out.println("Credential " + credential.path("id").asLong() + ": " + credential.path("status").asText());
        // docs:end
    }
}
