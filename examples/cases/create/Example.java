package examples.cases.create;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        JsonNode created = client.createCase(
                "Q4 2024 Vendor Invoices",
                "All invoices from vendors for Q4 2024 processing");

        System.out.println("Created case ID: " + created.path("id").asLong());
    }
}
// docs:end
