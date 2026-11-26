package examples.records.create_records_case;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.RecordsCase;
import ai.docudevs.client.RecordsCaseOptions;
// docs:end
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // One call creates the case and configures its records dataset.
        // Throws DocuDevsApiException when the case or the dataset is refused.
        RecordsCase records = client.createRecordsCase(
                "Product catalogue",
                "sku",
                Map.of("title", "title", "desc", "description"),
                RecordsCaseOptions.builder()
                        .description("Reference products for matching")
                        .label("category")
                        .metadata(Map.of("brand", "brand"))
                        .embeddedText("{title}\n{description}")
                        .indexSettings(Map.of("top_k", 10, "hybrid", false))
                        .build());
        System.out.println("Records case " + records.id());
        // docs:end
    }
}
