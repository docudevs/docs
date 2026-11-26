package examples.install.verify_connection;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.DocuDevsApiException;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // List your saved configurations to confirm the API key works.
        // The Java client throws on any status outside 2xx instead of returning it.
        try {
            client.listConfigurations();
            System.out.println("Connection successful! Status code: 200");
        } catch (DocuDevsApiException error) {
            System.out.println("Connection failed! Status code: " + error.getStatusCode());
        }
        // docs:end
    }
}
