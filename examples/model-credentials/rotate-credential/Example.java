package examples.model_credentials.rotate_credential;

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
        // credentialId is the "id" returned when the credential was created or listed.
        // NEW_PROVIDER_API_KEY holds the replacement secret.
        long credentialId = 42L;

        JsonNode rotated = client.rotateModelCredential(credentialId, System.getenv("NEW_PROVIDER_API_KEY"));
        System.out.println("Credential version: " + rotated.path("credentialVersion").asLong());
        // docs:end
    }
}
