package examples.templates.delete_template;

import ai.docudevs.client.DocuDevsClient;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        client.deleteTemplate("invoice");
        System.out.println("Deleted template invoice");
        // docs:end
    }
}
