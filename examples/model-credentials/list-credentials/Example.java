package examples.model_credentials.list_credentials;

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
        JsonNode credentials = client.listModelCredentials();
        for (JsonNode credential : credentials) {
            System.out.println(credential.path("id").asLong() + ": " + credential.path("name").asText()
                    + " (" + credential.path("status").asText() + ")");
        }
        // docs:end
    }
}
