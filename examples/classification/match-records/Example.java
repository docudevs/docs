package examples.classification.match_records;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the rows; it may differ from the catalogue

        // Throws DocuDevsApiException when the request is refused
        JsonNode accepted = client.classify(
                caseId,
                List.of(Map.of("key", "line-1", "text", Map.of("title", "M8 hex bolt 40 mm, 100 pcs"))),
                ClassifyOptions.builder()
                        .target("record")
                        .recordsKnowledgeBaseId(51L) // id of the catalogue's records case
                        .candidateStrategy(Map.of("similarRecords", Map.of("k", 20)))
                        .instructions("Match the invoice line to the catalogue product with the same item and pack size.")
                        .routing(Map.of("reviewWhen", List.of("declined", "modelNamedDifferentBestFit")))
                        .build());
        System.out.println("Classify job " + accepted.path("jobId").asText() + ": " + accepted.path("status").asText());
        // docs:end
    }
}
