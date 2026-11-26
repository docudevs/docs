package examples.model_credentials.create_entra_credential;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ModelCredentialOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // ENTRA_CLIENT_SECRET holds the client secret value copied from Entra.
        // The call returns the JSON body of the 201 response and throws DocuDevsApiException on an error status.
        JsonNode credential = client.createModelCredential(
                "Enterprise AI gateway",
                System.getenv("ENTRA_CLIENT_SECRET"),
                ModelCredentialOptions.builder()
                        .authType("ENTRA_CLIENT_SECRET")
                        .description("Shared by chat and embeddings")
                        .tenantId("00000000-0000-0000-0000-000000000000")
                        .clientId("11111111-1111-1111-1111-111111111111")
                        .scope("api://22222222-2222-2222-2222-222222222222/.default")
                        .cloud("PUBLIC")
                        .secretExpiresAt("2027-08-25T00:00:00Z")
                        .build());
        System.out.println("Credential " + credential.path("id").asLong() + ": " + credential.path("status").asText());
        // docs:end
    }
}
