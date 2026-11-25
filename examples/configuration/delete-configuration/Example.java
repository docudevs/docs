package examples.configuration.delete_configuration;

import ai.docudevs.client.DocuDevsClient;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        client.deleteConfiguration("invoice-config");
        System.out.println("Deleted configuration invoice-config");
        // docs:end
    }
}
