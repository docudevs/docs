package examples.errors.unauthorized;

// docs:start
import ai.docudevs.client.DocuDevsApiException;
import ai.docudevs.client.DocuDevsClient;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey("invalid-key")
                .baseUrl("https://api.docudevs.ai")
                .build();

        try {
            client.listCases();
        } catch (DocuDevsApiException error) {
            if (error.getStatusCode() == 401) {
                System.out.println("Authentication failed (401)");
            } else {
                throw error;
            }
        }
        // docs:end
    }
}
