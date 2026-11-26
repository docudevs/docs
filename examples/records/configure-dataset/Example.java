package examples.records.configure_dataset;

// docs:start
import ai.docudevs.client.DatasetOptions;
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id of a case that holds no documents

        // Throws DocuDevsApiException when the request is refused
        JsonNode dataset = client.setDataset(
                caseId,
                Map.of(
                        "key", "sku",
                        "text", Map.of("title", "title", "desc", "description"),
                        "label", "category",
                        "metadata", Map.of("brand", "brand")),
                DatasetOptions.builder()
                        .embeddedText(Map.of("template", "{title}\n{description}"))
                        .indexSettings(Map.of("top_k", 10, "hybrid", false))
                        .build());
        System.out.println("Mapping version: " + dataset.path("mappingVersion").asText());
        // docs:end
    }
}
