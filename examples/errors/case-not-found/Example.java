package examples.errors.case_not_found;

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
        try {
            client.getCase(99999);
        } catch (DocuDevsApiException error) {
            if (error.getStatusCode() == 404) {
                System.out.println("Case not found");
            } else {
                throw error;
            }
        }
        // docs:end
    }
}
