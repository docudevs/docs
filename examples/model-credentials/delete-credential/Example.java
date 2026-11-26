package examples.model_credentials.delete_credential;

import ai.docudevs.client.DocuDevsClient;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // credentialId is the "id" returned when the credential was created or listed.
        long credentialId = 42L;

        // Returns normally on 204 when deleted; throws DocuDevsApiException with status 409 while a provider still references the credential
        client.deleteModelCredential(credentialId);
        System.out.println("Deleted credential " + credentialId);
        // docs:end
    }
}
